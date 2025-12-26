def call (body) {
  def settings = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = settings
  body()

  container('python') {
    sh '''
      set -e
      set +x

      echo ""
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "🧪 PYTHON UNIT TESTS"
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

      REPOSITORY=$(echo ${JOB_NAME%/*} | tr '[:upper:]' '[:lower:]')

      echo "📋 Test Configuration:"
      echo "   • Branch:       ${GIT_BRANCH}"
      echo "   • Repository:   ${REPOSITORY}"
      echo "   • Commit:       $(echo ${GIT_COMMIT} | cut -c1-10)"
      echo "   • Python:       $(python --version)"
      echo "   • Pip:          $(pip --version)"
      echo ""

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "📦 Step 1/3: Installing dependencies..."
      echo ""

      pip install -r requirements.txt --quiet
      echo "   ✓ Dependencies installed"
      echo ""

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "🧪 Step 2/3: Running unit tests..."
      echo ""

      pytest tests/ -v --tb=short --cov=application --cov-report=term-missing

      if [ $? -eq 0 ]; then
        echo ""
        echo "   ✅ All unit tests passed"
      else
        echo ""
        echo "   ❌ Unit tests failed"
        exit 1
      fi
      echo ""

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "📊 Step 3/3: Generating coverage report..."
      echo ""

      pytest tests/ --cov=application --cov-report=xml --cov-report=html

      if [ $? -eq 0 ]; then
        echo "   ✓ Coverage report generated"
        echo "   • XML report: coverage.xml"
        echo "   • HTML report: htmlcov/"
      else
        echo "   ❌ Coverage report generation failed"
        exit 1
      fi
      echo ""

      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "✅ PYTHON UNIT TESTS COMPLETED SUCCESSFULLY!"
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo ""
      echo "📝 Test Summary:"
      echo "   • Branch:           ${GIT_BRANCH}"
      echo "   • Commit:           $(echo ${GIT_COMMIT} | cut -c1-10)"
      echo "   • Tests:            ✅ Passed"
      echo "   • Coverage:         ✅ Generated"
      echo "   • Repository:       ${REPOSITORY}"
      echo ""
      echo "🎉 ${REPOSITORY} unit tests are passing!"
      echo ""
    '''
  }
}
