#!/usr/bin/env bash
# ============================================================
# Publica o projeto Arena AI no GitHub:
#   1) cria o repositório
#   2) envia o código-fonte
#   3) cria a Release v1.0 com o APK pronto
#
# Uso:
#   GITHUB_TOKEN=ghp_seu_token ./publicar-no-github.sh [nome-do-repo]
#
# Token: GitHub > Settings > Developer settings > Tokens clássicos
#        com escopo "repo" (revogue depois do uso).
# ============================================================
set -euo pipefail

TOKEN="${GITHUB_TOKEN:?Defina GITHUB_TOKEN com um PAT com escopo 'repo'}"
REPO_NAME="${1:-arena-ai-android}"
API="https://api.github.com"
AUTH="Authorization: Bearer $TOKEN"

echo "1/5 Descobrindo a conta..."
OWNER=$(curl -s -H "$AUTH" "$API/user" | python3 -c "import sys,json;print(json.load(sys.stdin)['login'])")
echo "    Conta: $OWNER"

echo "2/5 Criando o repositório $OWNER/$REPO_NAME..."
curl -s -H "$AUTH" -H "Accept: application/vnd.github+json" \
  -d "{\"name\":\"$REPO_NAME\",\"description\":\"App Android para abrir o arena.ai como aplicativo dedicado\",\"has_issues\":true,\"has_wiki\":false}" \
  "$API/user/repos" | python3 -c "import sys,json;d=json.load(sys.stdin);print('   ',d.get('html_url') or d)"

echo "3/5 Enviando o código-fonte..."
cd "$(dirname "$0")"
git remote remove origin 2>/dev/null || true
git branch -M main
git push "https://x-access-token:${TOKEN}@github.com/${OWNER}/${REPO_NAME}.git" main

echo "4/5 Criando a Release v1.0..."
RELEASE_ID=$(curl -s -H "$AUTH" -H "Accept: application/vnd.github+json" \
  -d '{"tag_name":"v1.0","name":"Arena AI v1.0","body":"Primeira versão do app Arena AI para Android.\n\n- Abre https://arena.ai/ como app dedicado\n- Login, downloads, pull-to-refresh, tela de erro\n- Testes: 8/8 aprovados (Robolectric) — ver TEST-REPORT.md\n\nInstale o ArenaAI-v1.0.apk (Android 7.0+)"}' \
  "$API/repos/$OWNER/$REPO_NAME/releases" | python3 -c "import sys,json;print(json.load(sys.stdin)['id'])")
echo "    Release ID: $RELEASE_ID"

echo "5/5 Fazendo upload do APK na Release..."
curl -s -H "$AUTH" -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary @releases/ArenaAI-v1.0.apk \
  "https://uploads.github.com/repos/$OWNER/$REPO_NAME/releases/$RELEASE_ID/assets?name=ArenaAI-v1.0.apk" \
  | python3 -c "import sys,json;d=json.load(sys.stdin);print('    APK:',d.get('browser_download_url') or d)"

echo ""
echo "✅ Concluído! Acesse: https://github.com/$OWNER/$REPO_NAME"
