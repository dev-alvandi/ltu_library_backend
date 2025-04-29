package com.noahalvandi.dbbserver.util;

public class EmailTemplates {

    public static String getHtmlTemplate(String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
                <head>
                  <style>
                    .container {
                      font-family: Arial, sans-serif;
                      background-color: #f9f9f9;
                      padding: 30px;
                      border-radius: 10px;
                      max-width: 500px;
                      margin: auto;
                      box-shadow: 0 0 10px rgba(0,0,0,0.1);
                    }
                    .btn {
                      display: inline-block;
                      padding: 10px 20px;
                      margin-top: 20px;
                      background-color: #0F427E;
                      color: white;
                      text-decoration: none;
                      border-radius: 5px;
                    }
                    .footer {
                      font-size: 12px;
                      color: #999;
                      margin-top: 30px;
                    }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <h2>Password Reset Request</h2>
                    <p>You requested to reset your password. Click the button below to continue:</p>
                    <a href="%s" class="btn">Reset Password</a>
                    <p>If you didn't request this, you can ignore this email.</p>
                    <div class="footer">This link will expire in %d minutes.</div>
                  </div>
                </body>
            </html>
        """.formatted(resetLink, GlobalConstants.MINUTES_TO_EXPIRE_PASSWORD_TOKEN);
    }

    public static String getAccountDeletionHtmlTemplate(String firstName) {
        return """
        <!DOCTYPE html>
        <html>
            <head>
              <style>
                .container {
                  font-family: Arial, sans-serif;
                  background-color: #f9f9f9;
                  padding: 30px;
                  border-radius: 10px;
                  max-width: 500px;
                  margin: auto;
                  box-shadow: 0 0 10px rgba(0,0,0,0.1);
                }
                .footer {
                  font-size: 12px;
                  color: #999;
                  margin-top: 30px;
                }
              </style>
            </head>
            <body>
              <div class="container">
                <h2>Goodbye, %s!</h2>
                <p>We are truly grateful for the time you spent with us at Luleå University Library.</p>
                <p>Your account has been successfully deleted from our system.</p>
                <p>If you ever wish to return, we would love to have you back.</p>
                <p>Wishing you all the best on your journey ahead!</p>
                <div class="footer">Best regards,<br><strong>The Luleå University Library Team</strong></div>
              </div>
            </body>
        </html>
    """.formatted(firstName);
    }
}
