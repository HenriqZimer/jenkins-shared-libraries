def call(String recipientEmail) {
    script {
        // --- 1. Detecta o Status (Sucesso ou Falha) ---
        // Se o result for null, significa que chegou até aqui sem erros, então é SUCCESS
        def buildStatus = currentBuild.result ?: 'SUCCESS'

        // --- 2. Define Cores e Ícones baseado no Status ---
        def statusColor
        def statusIcon
        def statusMessage

        if (buildStatus == 'SUCCESS') {
            statusColor = '#2ecc71' // Verde
            statusIcon = '✅'
            statusMessage = 'Sucesso'
        } else if (buildStatus == 'UNSTABLE') {
            statusColor = '#f1c40f' // Amarelo
            statusIcon = '⚠️'
            statusMessage = 'Instável'
        } else {
            statusColor = '#e74c3c' // Vermelho
            statusIcon = '❌'
            statusMessage = 'Falha'
        }

        // --- 3. Monta o Assunto ---
        def subject = "[${statusMessage}] ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}"

        // --- 4. Monta o HTML (O mesmo template bonito) ---
        def bodyContent = """
            <div style="font-family: Arial, sans-serif; border: 1px solid #ccc; padding: 20px; border-radius: 5px;">
                <h2 style="color: ${statusColor}; margin-top: 0;">
                    ${statusIcon} Build ${statusMessage}
                </h2>
                <p>A build <strong>#${env.BUILD_NUMBER}</strong> do projeto <strong>${env.JOB_NAME}</strong> foi finalizada.</p>

                <table style="width: 100%; border-collapse: collapse; margin-top: 15px;">
                    <tr style="background-color: #f2f2f2;">
                        <td style="padding: 10px; border: 1px solid #ddd;"><strong>⏱ Duração:</strong></td>
                        <td style="padding: 10px; border: 1px solid #ddd;">${currentBuild.durationString}</td>
                    </tr>
                    <tr>
                        <td style="padding: 10px; border: 1px solid #ddd;"><strong>🔗 URL:</strong></td>
                        <td style="padding: 10px; border: 1px solid #ddd;"><a href="${env.BUILD_URL}">Ver Build no Jenkins</a></td>
                    </tr>
                    <tr style="background-color: #f2f2f2;">
                        <td style="padding: 10px; border: 1px solid #ddd;"><strong>💻 Node:</strong></td>
                        <td style="padding: 10px; border: 1px solid #ddd;">${env.NODE_NAME}</td>
                    </tr>
                </table>

                <br>
                <div style="text-align: center;">
                    <a href="${env.BUILD_URL}console" style="background-color: ${statusColor}; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">
                        Ver Logs do Console
                    </a>
                </div>
                <br>
                <p style="font-size: 10px; color: #999;">Enviado automaticamente pelo Jenkins CI.</p>
            </div>
        """

        // --- 5. Envia o E-mail ---
        emailext(
            to: recipientEmail,
            subject: subject,
            body: bodyContent,
            mimeType: 'text/html'
        )
    }
}
