def call (body) {
  def settings = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = settings
  body()

  container('crane') {
    sh '''
      set -e
      set +x

      echo ""
      echo "════════════════════════════════════════════════════════════════"
      echo "🏷️  ARTIFACT PROMOTION"
      echo "════════════════════════════════════════════════════════════════"

      REGISTRY=${REGISTRY}
      REPOSITORY=$(echo ${JOB_NAME%/*} | tr '[:upper:]' '[:lower:]')
      PROJECT=${PROJECT}
      PROJECT_MODE=${PROJECT_MODE}

      OLD_TAG=""
      TAG=""
      ENVIRONMENT=""

      echo "📋 Branch Information:"
      echo "   • Current Branch: ${GIT_BRANCH}"
      echo "   • Repository:     ${REPOSITORY}"
      echo "   • Registry:       ${REGISTRY}"
      echo ""

      if [ $(echo $GIT_BRANCH | grep -E "^release-.*") ]; then
        echo "🔄 Promotion Type: Development → Staging"
        OLD_TAG="$(cat /artifacts/${PROJECT}/${PROJECT_MODE}/dev.artifact)"
        ENVIRONMENT="stg"
        TAG="${GIT_BRANCH#*-}-$(echo ${OLD_TAG} | cut -d - -f 2)"
        echo "   • Source Environment:      Development"
        echo "   • Target Environment:      Staging"
      elif [ $(echo $GIT_BRANCH | grep -E "v[0-9]\\.[0-9]{1,2}\\.[0-9]{1,3}$") ]; then
        echo "🔄 Promotion Type: Staging → Production"
        OLD_TAG="$(cat /artifacts/${PROJECT}/${PROJECT_MODE}/stg.artifact)"
        ENVIRONMENT="pro"
        TAG="$(echo ${OLD_TAG} | cut -d - -f 1)"
        echo "   • Source Environment:      Staging"
        echo "   • Target Environment:      Production"
      else
        echo "❌ ERROR: Branch '${GIT_BRANCH}' does not match promotion patterns"
        echo "   Expected: release-* or v*.*.* "
        exit 1
      fi

      echo ""
      echo "════════════════════════════════════════════════════════════════"
      echo "📦 Artifact Details:"
      echo "   • Old Tag:  ${OLD_TAG}"
      echo "   • New Tag:  ${TAG}"
      echo "   • Image:    ${REGISTRY}/${PROJECT}/${PROJECT_MODE}"
      echo ""

      OLD_DESTINATION="${REGISTRY}/${PROJECT}/${PROJECT_MODE}:${OLD_TAG}"
      NEW_DESTINATION="${REGISTRY}/${PROJECT}/${PROJECT_MODE}:${TAG}"

      echo "🚀 Promoting artifact..."
      echo "   From: ${OLD_DESTINATION}"
      echo "   To:   ${NEW_DESTINATION}"
      echo ""

      crane tag ${OLD_DESTINATION} ${TAG}

      CRANE_EXIT_CODE=$?

      if [ $CRANE_EXIT_CODE -eq 0 ]; then
        echo "✅ Artifact promoted successfully!"
        echo "   • Tag '${TAG}' created for ${ENVIRONMENT} environment"
        echo "   • Artifact file saved: /artifacts/${PROJECT}/${PROJECT_MODE}/${ENVIRONMENT}.artifact"
        echo "${TAG}" > /artifacts/${PROJECT}/${PROJECT_MODE}/${ENVIRONMENT}.artifact
      else
        echo "❌ ERROR: Failed to promote artifact"
        echo "   Exit code: ${CRANE_EXIT_CODE}"
        exit 1
      fi

      echo "════════════════════════════════════════════════════════════════"
      echo ""
    '''
  }
}
