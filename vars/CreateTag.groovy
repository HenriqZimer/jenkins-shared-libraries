def call (body) {
  def settings = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = settings
  body()

  container('alpine-harbor-scanner') {
    sh '''
      set -e
      set +x

      echo ""
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "🏷️  CREATE PRODUCTION TAG"
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

      GIT_URL=${GIT_URL}
      GIT_EMAIL=${GIT_EMAIL}
      GIT_USER=${GIT_USER}
      GIT_SSH_URL=${GIT_SSH_URL}
      PROJECT_MODE=${PROJECT_MODE}
      PORT=${PORT}

      echo "📦 Installing dependencies..."
      apk add openssh git >/dev/null 2>&1
      echo "   ✓ openssh and git installed"
      echo ""

      RELEASE_VERSION="$(cat /artifacts/${PROJECT}/${PROJECT_MODE}/stg.artifact | cut -d - -f 1)"

      echo "📋 Tag Configuration:"
      echo "   • Branch:       ${GIT_BRANCH}"
      echo "   • Commit:       $(echo ${GIT_COMMIT} | cut -c1-10)"
      echo "   • Release Tag:  ${RELEASE_VERSION}"
      echo ""

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "🔧 Step 1/2: Configuring Git and SSH..."
      echo ""

      mkdir -p $HOME/.ssh
      cp $JENKINS_SSH_PRIVATE_KEY $HOME/.ssh/id_rsa
      chmod 400 $HOME/.ssh/id_rsa
      ssh-keyscan $GIT_URL > $HOME/.ssh/known_hosts 2>/dev/null

      git config --global user.email "$GIT_EMAIL"
      git config --global user.name "$GIT_USER"
      git config --global --add safe.directory $WORKSPACE

      echo "   ✓ Git and SSH configured"
      echo ""

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "🏷️  Step 2/2: Creating and pushing tag..."
      echo ""

      # Check if tag already exists locally
      if git rev-parse $RELEASE_VERSION >/dev/null 2>&1; then
        echo "   ⚠️  Tag $RELEASE_VERSION already exists locally, removing it"
        git tag -d $RELEASE_VERSION
      fi

      git tag -a $RELEASE_VERSION -m "production release: $RELEASE_VERSION"
      git push --tags

      if [ $? -eq 0 ]; then
        echo ""
        echo "   ✅ Tag created and pushed successfully!"
      else
        echo ""
        echo "   ❌ Failed to create/push tag"
        exit 1
      fi

      echo ""
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "✅ TAG CREATION COMPLETED!"
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo ""
      echo "📝 Summary:"
      echo "   • Tag:    ${RELEASE_VERSION}"
      echo "   • Branch: ${GIT_BRANCH}"
      echo "   • Status: ✅ Pushed to remote"
      echo ""
    '''
  }
}

