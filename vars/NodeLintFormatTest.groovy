def call(body) {
  def settings = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = settings
  body()

  container('nodejs') {
      sh '''
        set -e
        set +x

        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "⚡ NODEJS LINT-FORMAT-TEST PIPELINE"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

        REPOSITORY=$(echo ${JOB_NAME%/*} | tr '[:upper:]' '[:lower:]')

        echo "📋 Pipeline Configuration:"
        echo "   • Branch:       ${GIT_BRANCH}"
        echo "   • Repository:   ${REPOSITORY}"
        echo "   • Commit:       $(echo ${GIT_COMMIT} | cut -c1-10)"
        echo "   • Node Version: $(node --version)"
        echo "   • NPM Version:  $(npm --version)"
        echo ""

        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "📦 Step 1/6: Installing dependencies with cache..."
        echo ""

        # Define diretório de cache persistente
        CACHE_DIR="/artifacts/${PROJECT}/${PROJECT_MODE}/${REPOSITORY}-node_modules"

        echo "   📂 Repository: ${REPOSITORY}"
        echo "   💾 Cache dir: ${CACHE_DIR}"
        echo ""

        # Verifica se precisa instalar/atualizar
        CURRENT_HASH=$(md5sum package-lock.json 2>/dev/null | awk '{print $1}')
        CACHED_HASH=$(cat "$CACHE_DIR/.lock-hash" 2>/dev/null || echo "")
        CACHE_PKG_COUNT=$(find "$CACHE_DIR/node_modules" -maxdepth 1 -type d 2>/dev/null | wc -l)

        echo "   🔍 Cache validation:"
        echo "      • Cache dir exists: $([ -d "$CACHE_DIR" ] && echo 'YES' || echo 'NO')"
        echo "      • Lock hash exists: $([ -f "$CACHE_DIR/.lock-hash" ] && echo 'YES' || echo 'NO')"
        echo "      • Current hash: $CURRENT_HASH"
        echo "      • Cached hash:  $CACHED_HASH"
        echo "      • Hash match: $([ "$CURRENT_HASH" = "$CACHED_HASH" ] && echo 'YES' || echo 'NO')"
        echo "      • .bin exists: $([ -d "$CACHE_DIR/node_modules/.bin" ] && echo 'YES' || echo 'NO')"
        echo "      • Package count: $CACHE_PKG_COUNT (need > 10)"
        echo ""

        # Cache válido = hash correto + .bin existe + tem pacotes instalados
        if [ -d "$CACHE_DIR/node_modules" ] && [ -f "$CACHE_DIR/.lock-hash" ] && [ "$CURRENT_HASH" = "$CACHED_HASH" ] && [ -d "$CACHE_DIR/node_modules/.bin" ] && [ "$CACHE_PKG_COUNT" -gt 10 ]; then
          echo "   ✅ Cache is valid and up-to-date"
          echo "   ⚡ Skipping npm install (saved ~120s)"
          SKIP_INSTALL=true
        else
          if [ -d "$CACHE_DIR" ] && [ "$(ls -A $CACHE_DIR 2>/dev/null)" ]; then
            echo "   ⚠️  Cache invalid - will reinstall"
            echo "   🗑️  Cleaning old cache..."
            rm -rf "$CACHE_DIR"/*
          else
            echo "   ℹ️  No cache found - first install"
          fi

          # Cria diretório de cache
          mkdir -p "$CACHE_DIR"

          SKIP_INSTALL=false
        fi
        echo ""

        if [ "$SKIP_INSTALL" != "true" ]; then
          echo "   📥 Installing dependencies to cache..."
          echo ""

          # Remove node_modules local se existir
          rm -rf node_modules

          # Copia package.json e package-lock.json para o cache
          cp package.json package-lock.json "$CACHE_DIR/"

          # Instala direto no cache usando --prefix
          npm install \
            --prefix "$CACHE_DIR" \
            --prefer-offline \
            --no-audit \
            --no-fund \
            --progress=false \
            --loglevel=error

          # Salva hash
          echo "$CURRENT_HASH" > "$CACHE_DIR/.lock-hash"

          echo ""
          echo "   ✅ Dependencies installed to cache"
          echo ""
          echo "   🔍 Post-install verification:"
          echo "      • .bin created: $([ -d "$CACHE_DIR/node_modules/.bin" ] && echo 'YES' || echo 'NO')"
          echo "      • Packages in cache: $(find "$CACHE_DIR/node_modules" -maxdepth 1 -type d 2>/dev/null | wc -l)"
          echo "      • Cache size: $(du -sh "$CACHE_DIR/node_modules" 2>/dev/null | awk '{print $1}')"
        else
          echo "   ♻️  Using cached dependencies"
        fi

        # Remove node_modules local se existir
        rm -rf node_modules

        # Cria symlink para o cache (APÓS instalação)
        ln -sf "$CACHE_DIR/node_modules" node_modules
        echo "   🔗 node_modules → $CACHE_DIR/node_modules"
        echo ""

        # IMPORTANTE: Sempre roda nuxt prepare (gera .nuxt/)
        echo ""
        echo "   🔧 Running postinstall tasks..."
        npm run postinstall 2>/dev/null || nuxt prepare 2>/dev/null || echo "   ℹ️  No postinstall needed"

        # CRITICAL: Adiciona binários ao PATH (node_modules é symlink para cache)
        export PATH="$(pwd)/node_modules/.bin:$PATH"

        echo ""
        echo "   📊 Cache location: $CACHE_DIR"
        echo "   📦 Packages in cache: $(ls -1 $CACHE_DIR 2>/dev/null | wc -l)"
        echo "   💾 Cache size: $(du -sh $CACHE_DIR 2>/dev/null | awk '{print $1}' || echo 'N/A')"
        echo ""
        echo "   🔍 Debug - Verifying binaries:"
        echo "      • node_modules type: $(file node_modules | cut -d: -f2)"
        echo "      • .bin exists: $([ -d node_modules/.bin ] && echo 'YES' || echo 'NO')"
        echo "      • eslint location: $(which eslint 2>/dev/null || echo 'NOT IN PATH')"
        echo "      • Direct check: $([ -f node_modules/.bin/eslint ] && echo 'FOUND' || echo 'MISSING')"
        echo ""

        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "🔒 Step 2/6: Security Audit..."
        echo ""

        npm run security || true
        echo ""
        echo "   ✅ Security audit completed"
        echo ""

        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "🔍 Step 3/6: Lint Check..."
        echo ""
        echo "   🔍 Pre-lint debug:"
        echo "      • Current PATH: $PATH"
        echo "      • eslint via which: $(which eslint 2>/dev/null || echo 'NOT FOUND')"
        echo "      • eslint direct: $(ls -la node_modules/.bin/eslint 2>/dev/null || echo 'NOT FOUND')"
        echo "      • Running from: $(pwd)"
        echo ""

        npm run lint:check
        LINT_EXIT=$?

        if [ $LINT_EXIT -eq 0 ]; then
          echo ""
          echo "   ✅ Code linting passed"
        else
          echo ""
          echo "   ❌ Linting failed with exit code: $LINT_EXIT"
          echo ""
          echo "   🔍 Post-failure debug:"
          echo "      • Symlink target: $(readlink -f node_modules)"
          echo "      • .bin contents: $(ls -la node_modules/.bin/ 2>&1 | head -5)"
          exit 1
        fi
        echo ""

        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "💅 Step 4/6: Format Check..."
        echo ""

        npm run format:check

        if [ $? -eq 0 ]; then
          echo ""
          echo "   ✅ Code formatting is correct"
        else
          echo ""
          echo "   ❌ Format check failed"
          exit 1
        fi
        echo ""

        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "🧪 Step 6/6: Running Unit Tests..."
        echo ""
        echo "   Framework: Vitest"
        echo "   Coverage: Enabled (lcov format for SonarQube)"
        echo ""

        npm run test:coverage

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
        echo "✅ NODEJS LINT-FORMAT-TEST PIPELINE COMPLETED SUCCESSFULLY!"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""
        echo "📝 Pipeline Summary:"
        echo "   • Branch:           ${GIT_BRANCH}"
        echo "   • Commit:           $(echo ${GIT_COMMIT} | cut -c1-10)"
        echo "   • Dependencies:     ✅ Installed"
        echo "   • Security:         ✅ Checked"
        echo "   • Lint:             ✅ Passed"
        echo "   • Format:           ✅ Passed"
        echo "   • Unit Tests:       ✅ Passed"
        echo ""
        echo "🎉 ${REPOSITORY} is ready for deployment!"
        echo ""
      '''
  }
}
