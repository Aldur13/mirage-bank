import logging
import smtplib
from email.mime.text import MIMEText

import httpx

from config import settings

logger = logging.getLogger(__name__)


class EmailDeliveryError(Exception):
    """Raised when an email could not be sent through any configured channel."""


def send_email(to: str, subject: str, body: str) -> None:
    """Send a plain-text email.

    Prefers Mailgun, then Resend, then SMTP. Falls back to console logging
    when none are configured (dev mode).
    """
    if settings.mailgun_api_key:
        _send_via_mailgun(to, subject, body)
        return

    if settings.resend_api_key:
        _send_via_resend(to, subject, body)
        return

    if not settings.smtp_host:
        banner = "=" * 60
        print(f"\n{banner}\n[MIRAGE BANK — DEV EMAIL]\nTo:      {to}\nSubject: {subject}\n\n{body}\n{banner}\n", flush=True)
        logger.info("Email logged to console (no email provider configured). To: %s | Subject: %s", to, subject)
        return

    _send_via_smtp(to, subject, body)


def _send_via_mailgun(to: str, subject: str, body: str) -> None:
    try:
        resp = httpx.post(
            f"https://api.mailgun.net/v3/{settings.mailgun_domain}/messages",
            auth=("api", settings.mailgun_api_key),
            data={
                "from": f"noreply@{settings.mailgun_domain}",
                "to": to,
                "subject": subject,
                "text": body,
            },
            timeout=10,
        )
        resp.raise_for_status()
        logger.info("Email sent via Mailgun to %s: %s", to, subject)
    except httpx.HTTPError as exc:
        logger.error("Failed to send email via Mailgun to %s: %s", to, exc)
        raise EmailDeliveryError(str(exc)) from exc


def _send_via_resend(to: str, subject: str, body: str) -> None:
    try:
        resp = httpx.post(
            "https://api.resend.com/emails",
            headers={"Authorization": f"Bearer {settings.resend_api_key}"},
            json={"from": settings.smtp_from, "to": [to], "subject": subject, "text": body},
            timeout=10,
        )
        resp.raise_for_status()
        logger.info("Email sent via Resend to %s: %s", to, subject)
    except httpx.HTTPError as exc:
        logger.error("Failed to send email via Resend to %s: %s", to, exc)
        raise EmailDeliveryError(str(exc)) from exc


def _send_via_smtp(to: str, subject: str, body: str) -> None:
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
        logger.info("Email sent via SMTP to %s: %s", to, subject)
    except Exception as exc:
        logger.error("Failed to send email via SMTP to %s: %s", to, exc)
        raise EmailDeliveryError(str(exc)) from exc


def send_login_otp(email: str, code: str) -> None:
    subject = "Mirage Bank — Verification Code"
    body = (
        f"Your verification code is: {code}\n\n"
        "This code expires in 10 minutes. Do not share it with anyone.\n\n"
        "If you did not attempt to sign in, contact support immediately.\n\n"
        "— Mirage Bank Security"
    )
    send_email(email, subject, body)
