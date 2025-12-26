def call(body) {
  def settings = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = settings
  body()

  container('sonar-scanner-cli') {
    sh '''
      set +x  # Desabilita echo de comandos (mantém limpo)
      # NÃO usar set -e aqui, pois precisamos capturar o exit code do sonar-scanner

      echo ""
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "🔍 SONARQUBE STATIC CODE ANALYSIS"
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

      REPOSITORY=$(echo ${JOB_NAME%/*} | tr '[:upper:]' '[:lower:]')
      PROJECT_KEY="${REPOSITORY}-${GIT_BRANCH}"
      SONAR_URL=${SONAR_HOST_URL}

      echo "📋 Scan Configuration:"
      echo "   • Branch:       ${GIT_BRANCH}"
      echo "   • Repository:   ${REPOSITORY}"
      echo "   • Project Key:  ${PROJECT_KEY}"
      echo "   • Commit:       $(echo ${GIT_COMMIT} | cut -c1-10)"
      echo "   • SonarQube:    ${SONAR_URL}"
      echo ""

      echo "🚀 Starting code analysis..."
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

      # Executar sonar-scanner e capturar exit code (não falha o script)
      set +e  # Garante que não sai se houver erro
      sonar-scanner \
        -Dsonar.token=${SONAR_TOKEN} \
        -Dsonar.projectKey=${PROJECT_KEY} \
        -Dsonar.projectName="${PROJECT_KEY}" \
        -Dsonar.qualitygate.wait=true \
        -Dsonar.qualitygate.timeout=300

      SCAN_EXIT_CODE=$?
      set -e  # Reativa para comandos subsequentes

      echo ""
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

      if [ $SCAN_EXIT_CODE -eq 0 ]; then
        echo "✅ SonarQube Analysis PASSED!"
        echo "   Quality Gate: SUCCESS"
        echo "   📊 View report: ${SONAR_URL}/dashboard?id=${PROJECT_KEY}"
      else
        echo "❌ SonarQube Analysis FAILED!"
        echo "   Quality Gate: FAILED"
        echo "   📊 View issues: ${SONAR_URL}/dashboard?id=${PROJECT_KEY}"
        echo ""
        echo "💡 Common issues:"
        echo "   • Code coverage below threshold"
        echo "   • Code smells or bugs detected"
        echo "   • Security vulnerabilities found"
        echo "   • Duplicated code blocks"
        echo ""
        echo "   Check the SonarQube dashboard for detailed information."
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""
        exit 1  # Falha o pipeline
      fi

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo ""
    '''
  }
}
