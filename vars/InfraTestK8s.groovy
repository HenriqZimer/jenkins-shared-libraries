def call (body) {
  def settings = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = settings
  body()

  container('helm') {
    sh '''
      # set -e
      # set +x

      echo ""
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "🧪 INFRASTRUCTURE TESTS ON K8S"
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

      GIT_URL=${GIT_URL}
      GIT_SSH_URL=${GIT_SSH_URL}
      NAMESPACE=${NAMESPACE}
      PROJECT=${PROJECT}
      PROJECT_MODE=${PROJECT_MODE}
      PROJECT_MODE_DP=${PROJECT_MODE_DP}
      PORT=${PORT}
      HEALTHCHECK_ENDPOINT=${HEALTHCHECK_ENDPOINT}

      echo "📋 Test Configuration:"
      echo "   • Branch:       ${GIT_BRANCH}"
      echo "   • Project:      ${PROJECT}"
      echo "   • Commit:       $(echo ${GIT_COMMIT} | cut -c1-10)"
      echo ""

      # Determine environment
      ENVIRONMENT=""
      if [ $(echo $GIT_BRANCH | grep ^develop$) ]; then
        ENVIRONMENT="dev"
        echo "🏗️  Test Type: Development"
        echo "   • Environment:  Development"
      elif [ $(echo $GIT_BRANCH | grep -E "^hotfix-.*") ]; then
        ENVIRONMENT="stg"
        echo "🏗️  Test Type: Hotfix (Staging)"
        echo "   • Environment:  Staging"
      else
        echo "❌ ERROR: Branch '${GIT_BRANCH}' does not match test patterns"
        echo "   Expected: develop or hotfix-*"
        exit 1
      fi

      IMAGE_TAG="$(cat /artifacts/${PROJECT}/${PROJECT_MODE}/${ENVIRONMENT}.artifact)"
      IMAGE_TAG_DP="$(cat /artifacts/${PROJECT}/${PROJECT_MODE_DP}/${ENVIRONMENT}.artifact)"
      echo "   • Image Tag:    ${IMAGE_TAG}"
      echo ""

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "📦 Step 1/5: Installing dependencies..."
      echo ""
      apk add openssh >/dev/null 2>&1
      echo "   ✓ openssh installed"
      echo ""

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "🔐 Step 2/5: Configuring SSH..."
      echo ""

      mkdir -p $HOME/.ssh
      cp $JENKINS_SSH_PRIVATE_KEY $HOME/.ssh/id_rsa
      chmod 400 $HOME/.ssh/id_rsa
      ssh-keyscan ${GIT_URL} > $HOME/.ssh/known_hosts 2>/dev/null

      echo "   ✓ SSH keys configured"
      echo ""

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "📥 Step 3/5: Cloning helm repository..."
      echo ""

      git clone ${GIT_SSH_URL}
      echo "   ✓ Repository cloned"
      cd meu-site-helm-chart/chart
      echo "   ✓ Changed to CI directory"
      echo ""

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "⚙️  Step 4/5: Deploying test instance with Helm..."
      echo ""
      echo "   • Namespace:     ${NAMESPACE}"
      echo "   • Project:       ${PROJECT}"
      echo "   • Image Tag:     ${IMAGE_TAG}"
      echo ""

      helm upgrade --install --force ${PROJECT} ./ \
        --values values-ci.yaml \
        --namespace ${NAMESPACE} \
        --create-namespace \
        --set deployments.${PROJECT_MODE_DP}.image.tag="${IMAGE_TAG_DP}" \
        --set deployments.${PROJECT_MODE}.image.tag="${IMAGE_TAG}" \
        --wait \
        --timeout 5m

      if [ $? -eq 0 ]; then
        echo ""
        echo "   ✓ Helm deployment successful"
      else
        echo ""
        echo "   ❌ Helm deployment failed"
        exit 1
      fi
      echo ""

      sleep 20

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "🧪 Step 5/5: Running health check..."
      echo ""
      echo "   Testing endpoint: http://${PROJECT}-${PROJECT_MODE}.${NAMESPACE}.svc.cluster.local:${PORT}${HEALTHCHECK_ENDPOINT}"
      echo ""

      status_code="$(curl --silent \
        --output /dev/null \
        --write-out '%{http_code}\n' \
        "http://${PROJECT}-${PROJECT_MODE}.${NAMESPACE}.svc.cluster.local:${PORT}${HEALTHCHECK_ENDPOINT}")"

      if [ "$status_code" == "200" ]; then
        echo "   ✅ API health check passed!"
        echo "   • HTTP Status: ${status_code}"
        echo "   • Endpoint:    ${HEALTHCHECK_ENDPOINT}"
      else
        echo "   ❌ API health check failed!"
        echo "   • HTTP Status: ${status_code}"
        echo "   • Expected:    200"
        exit 1
      fi

      echo ""
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "✅ INFRASTRUCTURE TESTS COMPLETED SUCCESSFULLY!"
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo ""
      echo "📝 Test Summary:"
      echo "   • Environment:      ${ENVIRONMENT}"
      echo "   • Image Tag:        ${IMAGE_TAG}"
      echo "   • Health Check:     ✅ Passed"
      echo "   • Test Namespace:   ${NAMESPACE}"
      echo ""
      echo "⚠️  Remember to clean up test resources after pipeline completion"
      echo ""
    '''
  }
}

