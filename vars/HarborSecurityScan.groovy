def call (body) {
  def settings = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = settings
  body()

  container('alpine-harbor-scanner') {
    sh '''
      // set -e
      set +x

      echo ""
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "🛡️  HARBOR SECURITY SCAN"
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

      echo "📦 Installing dependencies..."
      apk add curl jq >/dev/null 2>&1
      echo "   ✓ curl and jq installed"
      echo ""

      # Retry backoff parameters
      MAX_RETRY=15
      COUNT=1
      SLEEP=5

      # Extract environment from branch
      ENVIRONMENT=""
      if echo "$GIT_BRANCH" | grep -q "^develop$"; then
        ENVIRONMENT="dev"
      elif echo "$GIT_BRANCH" | grep -qE "^hotfix-.*"; then
        ENVIRONMENT="stg"
      fi

      if [ -z "$ENVIRONMENT" ]; then
        echo "❌ ERROR: Could not determine environment from branch: $GIT_BRANCH"
        echo "   Supported branches: develop, hotfix-*"
        exit 1
      fi

      echo "📋 Scan Configuration:"
      echo "   • Branch:      ${GIT_BRANCH}"
      echo "   • Environment: ${ENVIRONMENT}"

      # Read artifact tag
      if [ ! -f "/artifacts/${PROJECT}/${PROJECT_MODE}/${ENVIRONMENT}.artifact" ]; then
        echo ""
        echo "❌ ERROR: Artifact file not found: /artifacts/${PROJECT}/${PROJECT_MODE}/${ENVIRONMENT}.artifact"
        exit 1
      fi

      TAG="$(cat /artifacts/${PROJECT}/${PROJECT_MODE}/${ENVIRONMENT}.artifact)"
      echo "   • Tag:         ${TAG}"

      # Harbor variables
      PROJECT=${PROJECT}
      REPOSITORY=$(echo "${JOB_NAME%/*}" | tr '[:upper:]' '[:lower:]')
      REGISTRY="https://${REGISTRY}"
      HARBOR_API_PATH="api/v2.0/projects/${PROJECT}/repositories/${PROJECT_MODE}/artifacts/${TAG}"

      echo "   • Repository:  ${REPOSITORY}"
      echo "   • Project:     ${PROJECT}"
      echo "   • Harbor URL:  ${REGISTRY}"
      echo ""
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

      # Step 1: Trigger scan if not already running
      echo "🚀 Step 1: Initiating vulnerability scan..."
      TRIGGER_RESPONSE=$(curl -s -w "\\nHTTP_CODE:%{http_code}" -X POST \\
        "${REGISTRY}/${HARBOR_API_PATH}/scan" \\
        -H "accept: application/json" \\
        -H "authorization: Basic ${HARBOR_CREDENTIALS}")

      HTTP_CODE=$(echo "$TRIGGER_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)

      case "$HTTP_CODE" in
        201)
          echo "   ✓ Scan started successfully"
          ;;
        202)
          echo "   ℹ️  Scan already in progress"
          ;;
        400)
          echo "   ℹ️  Artifact already scanned"
          ;;
        *)
          echo "   ⚠️  Unexpected HTTP response: ${HTTP_CODE}"
          ;;
      esac
      echo ""

      # Step 2: Wait for scan to complete
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "⏳ Step 2: Waiting for scan completion..."
      echo "   Maximum retries: ${MAX_RETRY}"
      echo ""

      SCAN_STATUS="not_ready"
      SEVERITY="Unknown"

      while [ "$COUNT" -le "$MAX_RETRY" ]; do
        echo "   [Attempt ${COUNT}/${MAX_RETRY}] Waiting ${SLEEP}s before checking..."
        sleep $SLEEP

        # Get artifact details with scan overview
        RESPONSE=$(curl -s -X GET \\
          "${REGISTRY}/${HARBOR_API_PATH}?with_scan_overview=true" \\
          -H "accept: application/json" \\
          -H "authorization: Basic ${HARBOR_CREDENTIALS}")

        # Check if scan_overview exists and is not null
        SCAN_EXISTS=$(echo "$RESPONSE" | jq -r 'if .scan_overview then "yes" else "no" end')

        if [ "$SCAN_EXISTS" = "yes" ]; then
          # Get scan status (Pending, Running, Success, Error, etc.)
          SCAN_STATUS=$(echo "$RESPONSE" | jq -r '.scan_overview | to_entries | .[].value.scan_status // "Unknown"')

          echo "   ├─ Status: ${SCAN_STATUS}"

          # Check if scan is complete
          if [ "$SCAN_STATUS" = "Success" ] || [ "$SCAN_STATUS" = "Error" ]; then
            # Get severity
            SEVERITY=$(echo "$RESPONSE" | jq -r '.scan_overview | to_entries | .[].value.severity // "Unknown"')
            echo "   └─ ✅ Scan completed!"
            echo ""
            break
          else
            echo "   └─ ⏳ Still scanning..."
          fi
        else
          echo "   └─ ⏳ Scan initializing..."
        fi

        # Exponential backoff
        SLEEP=$((SLEEP * 2))

        # Cap sleep at 60 seconds
        if [ $SLEEP -gt 60 ]; then
          SLEEP=60
        fi

        COUNT=$((COUNT + 1))
      done

      # Check if we reached max retries
      if [ "$COUNT" -gt "$MAX_RETRY" ]; then
        echo ""
        echo "❌ ERROR: Timeout - Reached maximum retry of ${MAX_RETRY} attempts"
        echo "   Last scan status: ${SCAN_STATUS}"
        echo "   Please check Harbor manually:"
        echo "   ${REGISTRY}/harbor/projects/${PROJECT}/repositories/${PROJECT_MODE}/artifacts-tab"
        exit 1
      fi

      # Evaluate severity
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "📊 Step 3: Evaluating scan results..."
      echo ""
      echo "   🔍 Vulnerability Severity: ${SEVERITY}"
      echo ""

      case "$SEVERITY" in
        "Critical")
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo "❌ CRITICAL VULNERABILITIES DETECTED!"
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo "   Pipeline FAILED due to critical security issues."
          echo ""
          echo "   📋 Action Required:"
          echo "      1. Review vulnerabilities in Harbor"
          echo "      2. Update affected dependencies"
          echo "      3. Rebuild and rescan the image"
          echo ""
          echo "   🔗 Detailed Report:"
          echo "      ${REGISTRY}/harbor/projects/${PROJECT}/repositories/${PROJECT_MODE}/artifacts-tab"
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo ""
          exit 1
          ;;
        "High")
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo "⚠️  HIGH SEVERITY VULNERABILITIES FOUND"
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo "   Pipeline will continue, but review is recommended."
          echo ""
          echo "   🔗 Review Report:"
          echo "      ${REGISTRY}/harbor/projects/${PROJECT}/repositories/${PROJECT_MODE}/artifacts-tab"
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo ""
          # Uncomment next line to fail on High severity:
          # exit 1
          ;;
        "Medium")
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo "✅ SCAN PASSED - Medium Severity"
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo "   Medium severity vulnerabilities detected."
          echo "   Consider reviewing and planning fixes."
          echo ""
          echo "   🔗 View Report:"
          echo "      ${REGISTRY}/harbor/projects/${PROJECT}/repositories/${PROJECT_MODE}/artifacts-tab"
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo ""
          ;;
        "Low")
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo "✅ SCAN PASSED - Low Severity"
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo "   Only low severity vulnerabilities detected."
          echo "   Image is safe to deploy."
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo ""
          ;;
        "None")
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo "✅ SCAN PASSED - No Vulnerabilities!"
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo "   🎉 No vulnerabilities detected!"
          echo "   Image is secure and ready to deploy."
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo ""
          ;;
        "Unknown"|*)
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo "⚠️  WARNING: Unknown Severity Status"
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo "   Could not determine vulnerability severity: ${SEVERITY}"
          echo "   Last scan status: ${SCAN_STATUS}"
          echo ""
          echo "   🔗 Manual verification required:"
          echo "      ${REGISTRY}/harbor/projects/${PROJECT}/repositories/${PROJECT_MODE}/artifacts-tab"
          echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
          echo ""
          # Decide if you want to fail on unknown - uncomment to fail:
          # exit 1
          ;;
      esac

      echo "📝 Scan Summary:"
      echo "   • Environment:  ${ENVIRONMENT}"
      echo "   • Tag:          ${TAG}"
      echo "   • Repository:   ${REPOSITORY}"
      echo "   • Severity:     ${SEVERITY}"
      echo "   • Status:       ${SCAN_STATUS}"
      echo ""
    '''
  }
}

