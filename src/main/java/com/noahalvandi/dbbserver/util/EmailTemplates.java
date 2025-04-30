package com.noahalvandi.dbbserver.util;

import java.time.Instant;
import java.time.LocalDateTime;

public class EmailTemplates {

    public static String getResetPasswordTemplate(String resetLink) {
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

    public static String getLoanReceiptTemplate(String firstName, String bookTitle, Instant loanDate, LocalDateTime dueDate, int dailyOverdueFee) {
        return """
        <!DOCTYPE html>
        <html>
            <head>
              <style>
                h2 {
                  text-transform: capitalize;
                }
                .container {
                  font-family: Arial, sans-serif;
                  background-color: #f9f9f9;
                  padding: 30px;
                  border-radius: 10px;
                  max-width: 600px;
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
                <h2>Hello, %s!</h2>
                <p>Thank you for borrowing from Luleå University Library.</p>
                <p><strong>Book Title:</strong> %s</p>
                <p><strong>Loan Date:</strong> %s</p>
                <p><strong>Due Date:</strong> %s</p>
                <p><strong>Overdue Fee:</strong> %d kr/day after the due date.</p>
                <p>Please return the item on time to avoid penalties.</p>
                <div class="footer">Best regards,<br><strong>The Luleå University Library Team</strong></div>
              </div>
            </body>
        </html>
    """.formatted(firstName, bookTitle, loanDate.toString(), dueDate.toString(), dailyOverdueFee);
    }

    public static String getReturnReceiptTemplate(String firstName, String title, LocalDateTime returnedAt, LocalDateTime dueAt, boolean isLate, int fee) {
        return """
        <!DOCTYPE html>
        <html>
            <head>
              <style>
                h2 {
                  text-transform: capitalize;
                }
                .container {
                  font-family: Arial, sans-serif;
                  background-color: #f9f9f9;
                  padding: 30px;
                  border-radius: 10px;
                  max-width: 600px;
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
                <h2>Hello, %s!</h2>
                <p>You have successfully returned the following resource:</p>
                <p><strong>Title:</strong> %s</p>
                <p><strong>Returned at:</strong> %s</p>
                <p><strong>Due date was:</strong> %s</p>
                %s
                <div class="footer">Thank you for using the Luleå University Library.</div>
              </div>
            </body>
        </html>
    """.formatted(
                firstName,
                title,
                returnedAt.toString(),
                dueAt.toString(),
                isLate ? "<p style='color:red'><strong>Overdue Fee:</strong> You owe " + fee + " kr for returning this item late.</p>" : ""
        );
    }

}
