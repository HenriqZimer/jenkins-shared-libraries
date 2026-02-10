def call (body) {
  def settings = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = settings
  body()

  container('kaniko') {
    sh '''
      set -e
      set +x

      echo ""
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "🐳 KANIKO BUILD & PUSH"
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

      REGISTRY=${REGISTRY}
      PROJECT=${PROJECT}
      REPOSITORY=$(echo ${JOB_NAME%/*} | tr '[:upper:]' '[:lower:]')
      PROJECT_MODE=${PROJECT_MODE}
      TAG=""
      ENVIRONMENT=""
      COMMIT_SHORT=$(echo ${GIT_COMMIT} | cut -c1-10)

      echo "📋 Build Configuration:"
      echo "   • Branch:       ${GIT_BRANCH}"
      echo "   • Repository:   ${REPOSITORY}"
      echo "   • Commit:       ${COMMIT_SHORT}"
      echo "   • Registry:     ${REGISTRY}"
      echo "   • Project:      ${PROJECT}"
      echo ""

      if [ $(echo $GIT_BRANCH | grep ^develop$) ]; then
        TAG="dev-${COMMIT_SHORT}"
        ENVIRONMENT="dev"
        echo "🏗️  Build Type: Development"
        echo "   • Environment:  Development"
        echo "   • Tag Pattern:  dev-<commit>"
      elif [ $(echo $GIT_BRANCH | grep -E "^hotfix-.*") ]; then
        BRANCH_NAME="${GIT_BRANCH#*-}"
        TAG="${BRANCH_NAME}-${COMMIT_SHORT}"
        ENVIRONMENT="stg"
        echo "🏗️  Build Type: Hotfix (Staging)"
        echo "   • Environment:  Staging"
        echo "   • Tag Pattern:  <branch>-<commit>"
      else
        echo "❌ ERROR: Branch '${GIT_BRANCH}' does not match build patterns"
        echo "   Expected: develop or hotfix-*"
        exit 1
      fi

      echo "   • Tag:          ${TAG}"
      echo ""

      DESTINATION="${REGISTRY}/${PROJECT}/${PROJECT_MODE}:${TAG}"

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "📦 Step 1/2: Building container image..."
      echo ""
      echo "   • Context:      $(pwd)"
      echo "   • Destination:  ${DESTINATION}"
      echo ""

      # Run Kaniko without cache
      /kaniko/executor \
        --destination "${DESTINATION}" \
        --context $(pwd) \
        --snapshot-mode=redo

      BUILD_EXIT_CODE=$?

      if [ $BUILD_EXIT_CODE -eq 0 ]; then
        echo ""
        echo "   ✅ Image built and pushed successfully"
        echo ""
      else
        echo ""
        echo "   ❌ Build failed"
        echo "   📋 Check build logs above for details"
        exit 1
      fi

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "💾 Step 2/2: Saving artifact metadata..."
      echo ""

      echo "${TAG}" > /artifacts/${PROJECT}/${PROJECT_MODE}/${ENVIRONMENT}.artifact

      if [ -f "/artifacts/${PROJECT}/${PROJECT_MODE}/${ENVIRONMENT}.artifact" ]; then
        echo "   ✅ Artifact file saved: /artifacts/${PROJECT}/${PROJECT_MODE}/${ENVIRONMENT}.artifact"
        echo "   • Tag saved: ${TAG}"
        echo ""
      else
        echo "   ❌ Failed to save artifact file"
        exit 1
      fi

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "✅ BUILD COMPLETED SUCCESSFULLY!"
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo ""
      echo "📝 Build Summary:"
      echo "   • Branch:       ${GIT_BRANCH}"
      echo "   • Commit:       ${COMMIT_SHORT}"
      echo "   • Environment:  ${ENVIRONMENT}"
      echo "   • Image Tag:    ${TAG}"
      echo "   • Registry:     ${REGISTRY}"
      echo "   • Full Image:   ${DESTINATION}"

      echo ""
      echo "🚀 Image is ready for deployment!"
      echo ""
    '''
  }
}
