def call (body) {
  def settings = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = settings
  body()

  container('alpine') {
    sh '''
      set -e
      set +x

      echo ""
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "🚀 DEPLOY TO PRODUCTION"
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

      IMAGE_TAG="$(cat /artifacts/${PROJECT}/${PROJECT_MODE}/pro.artifact)"
      GIT_URL=${GIT_URL}
      GIT_EMAIL=${GIT_EMAIL}
      GIT_USER=${GIT_USER}
      GIT_SSH_URL=${GIT_SSH_URL}
      PROJECT=${PROJECT}
      PROJECT_NAME=${PROJECT_NAME}

      echo "📋 Deployment Configuration:"
      echo "   • Environment:  Production"
      echo "   • Project:      ${PROJECT}"
      echo "   • Branch:       ${GIT_BRANCH}"
      echo "   • Image Tag:    ${IMAGE_TAG}"
      echo ""

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "📦 Step 1/4: Installing dependencies..."
      echo ""
      apk add openssh git >/dev/null 2>&1
      if ! command -v yq &> /dev/null; then
        wget https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64 -O /usr/bin/yq && chmod +x /usr/bin/yq
      fi
      echo "   ✓ openssh, git, and yq installed"
      echo ""

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "🔐 Step 2/4: Configuring SSH and Git..."
      echo ""

      mkdir -p $HOME/.ssh
      cp $JENKINS_SSH_PRIVATE_KEY $HOME/.ssh/id_rsa
      chmod 400 $HOME/.ssh/id_rsa
      ssh-keyscan $GIT_URL > $HOME/.ssh/known_hosts 2>/dev/null

      git config --global user.email "$GIT_EMAIL"
      git config --global user.name "$GIT_USER"

      echo "   ✓ SSH keys configured"
      echo "   ✓ Git user configured"
      echo ""

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "📥 Step 3/4: Cloning helm repository..."
      echo ""

      if [ ! -d ${PROJECT}-helm-chart ]; then
        git clone $GIT_SSH_URL
        echo "   ✓ Repository cloned"
      else
        echo "   ℹ️  Repository already exists"
      fi

      cd ${PROJECT}-helm-chart/chart
      echo "   ✓ Changed to CI directory"
      echo ""

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "📝 Step 4/4: Updating values and committing..."
      echo ""

      # Garantindo que a variável esteja disponível para o yq
      export TARGET_TAG="${IMAGE_TAG}"
      export TARGET_SERVICE="${PROJECT_MODE}"

      # Usando yq com env para evitar erros de lexer/parser
      yq -i ".[env(TARGET_SERVICE)].image.tag = env(TARGET_TAG)" values-pro.yaml

      echo "   ✓ Updated values-pro.yaml for ${PROJECT_MODE} with tag: ${IMAGE_TAG}"

      git add values-pro.yaml
      git commit -m "[${PROJECT}-${PROJECT_MODE}] - deploy ${IMAGE_TAG}" --allow-empty
      git push

      if [ $? -eq 0 ]; then
        echo "   ✓ Changes committed and pushed"
      else
        echo "   ❌ Failed to push changes"
        exit 1
      fi

      echo ""
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "✅ DEPLOYMENT TO PRODUCTION COMPLETED!"
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo ""
      echo "📝 Deployment Summary:"
      echo "   • Environment:  Production"
      echo "   • Project:      ${PROJECT}"
      echo "   • Image Tag:    ${IMAGE_TAG}"
      echo "   • Status:       ✅ Deployed"
      echo ""
    '''
  }
}
