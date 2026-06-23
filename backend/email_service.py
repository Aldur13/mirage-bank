import logging
import smtplib
from email.mime.text import MIMEText

from config import settings

logger = logging.getLogger(__name__)


def send_email(to: str, subject: str, body: str) -> None:
    """Send a plain-text email. Falls back to console logging when SMTP is not configured."""
    if not settings.smtp_host:
        banner = "=" * 60
        print(f"\n{banner}\n[MIRAGE BANK — DEV EMAIL]\nTo:      {to}\nSubject: {subject}\n\n{body}\n{banner}\n", flush=True)
        logger.info("Email logged to console (no SMTP configured). To: %s | Subject: %s", to, subject)
        return

    msg = MIMEText(body, "plain", "utf-8")
    msg["Subject"] = subject
    msg["From"] = settings.smtp_from
    msg["To"] = to

    try:
        with smtplib.SMTP(settings.smtp_host, settings.smtp_port, timeout=10) as server:
            server.ehlo()
            if settings.smtp_port == 587:
                server.starttls()
                server.ehlo()
            if settings.smtp_user:
                server.login(settings.smtp_user, settings.smtp_password)
            server.sendmail(settings.smtp_from, [to], msg.as_string())
        logger.info("Email sent to %s: %s", to, subject)
    except Exception as exc:
        logger.error("Failed to send email to %s: %s", to, exc)
        raise


def send_admin_otp(email: str, code: str) -> None:
    subject = "Mirage Bank — Admin Verification Code"
    body = (
        f"Your admin verification code is: {code}\n\n"
        "This code expires in 10 minutes. Do not share it with anyone.\n\n"
        "If you did not attempt to sign in, contact your security team immediately.\n\n"
        "— Mirage Bank Security"
    )
    send_email(email, subject, body)
