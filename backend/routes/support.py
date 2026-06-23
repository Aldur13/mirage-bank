import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, status

from database import get_session
from dependencies import get_current_user
from models import (
    TicketCreateRequest, TicketMessageCreate,
    TicketItem, TicketDetail, TicketListResponse,
    TicketMessageItem, TicketActionResponse,
)

router = APIRouter(prefix="/support")


def _fmt_ticket(r: dict) -> TicketItem:
    return TicketItem(
        id=r["id"], subject=r["subject"], status=r["status"],
        priority=r["priority"], created_at=r["created_at"],
        updated_at=r["updated_at"], message_count=r.get("message_count", 0),
    )


@router.post("/tickets", response_model=TicketDetail, status_code=status.HTTP_201_CREATED)
def create_ticket(body: TicketCreateRequest, current_user: dict = Depends(get_current_user)):
    ticket_id = str(uuid.uuid4())
    msg_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()

    with get_session() as session:
        session.run(
            """
            MATCH (u:User {id: $user_id})
            CREATE (tk:Ticket {
                id: $ticket_id, subject: $subject,
                status: 'open', priority: 'normal',
                created_at: $now, updated_at: $now
            })
            CREATE (u)-[:OWNS_TICKET]->(tk)
            CREATE (msg:TicketMessage {
                id: $msg_id, content: $content,
                author_id: $user_id, is_staff: false, created_at: $now
            })
            CREATE (msg)-[:IN]->(tk)
            """,
            user_id=current_user["id"], ticket_id=ticket_id,
            subject=body.subject, msg_id=msg_id, content=body.message, now=now,
        )

    return TicketDetail(
        id=ticket_id, subject=body.subject, status="open", priority="normal",
        created_at=now, updated_at=now,
        messages=[TicketMessageItem(
            id=msg_id, content=body.message,
            author_id=current_user["id"], author_name=current_user["name"],
            is_staff=False, created_at=now,
        )],
    )


@router.get("/tickets", response_model=TicketListResponse)
def list_tickets(current_user: dict = Depends(get_current_user)):
    with get_session() as session:
        records = session.run(
            """
            MATCH (u:User {id: $user_id})-[:OWNS_TICKET]->(tk:Ticket)
            OPTIONAL MATCH (msg:TicketMessage)-[:IN]->(tk)
            WITH tk, count(msg) AS message_count
            RETURN tk.id AS id, tk.subject AS subject, tk.status AS status,
                   tk.priority AS priority, tk.created_at AS created_at,
                   tk.updated_at AS updated_at, message_count
            ORDER BY tk.updated_at DESC
            """,
            user_id=current_user["id"],
        ).data()
    return TicketListResponse(tickets=[_fmt_ticket(r) for r in records])


@router.get("/tickets/{ticket_id}", response_model=TicketDetail)
def get_ticket(ticket_id: str, current_user: dict = Depends(get_current_user)):
    with get_session() as session:
        tk = session.run(
            """
            MATCH (u:User {id: $user_id})-[:OWNS_TICKET]->(tk:Ticket {id: $ticket_id})
            RETURN tk.id AS id, tk.subject AS subject, tk.status AS status,
                   tk.priority AS priority, tk.created_at AS created_at, tk.updated_at AS updated_at
            """,
            user_id=current_user["id"], ticket_id=ticket_id,
        ).single()
        if tk is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found")

        msgs = session.run(
            """
            MATCH (msg:TicketMessage)-[:IN]->(tk:Ticket {id: $ticket_id})
            MATCH (author:User {id: msg.author_id})
            RETURN msg.id AS id, msg.content AS content, msg.author_id AS author_id,
                   author.name AS author_name, msg.is_staff AS is_staff, msg.created_at AS created_at
            ORDER BY msg.created_at ASC
            """,
            ticket_id=ticket_id,
        ).data()

    return TicketDetail(
        id=tk["id"], subject=tk["subject"], status=tk["status"],
        priority=tk["priority"], created_at=tk["created_at"], updated_at=tk["updated_at"],
        messages=[TicketMessageItem(**m) for m in msgs],
    )


@router.post("/tickets/{ticket_id}/messages", response_model=TicketMessageItem, status_code=status.HTTP_201_CREATED)
def add_message(ticket_id: str, body: TicketMessageCreate, current_user: dict = Depends(get_current_user)):
    with get_session() as session:
        owns = session.run(
            "MATCH (u:User {id: $user_id})-[:OWNS_TICKET]->(tk:Ticket {id: $ticket_id}) "
            "WHERE tk.status NOT IN ['resolved','closed'] RETURN tk.id",
            user_id=current_user["id"], ticket_id=ticket_id,
        ).single()
        if owns is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND,
                                detail="Ticket not found or is closed")

        msg_id = str(uuid.uuid4())
        now = datetime.now(timezone.utc).isoformat()
        session.run(
            """
            MATCH (tk:Ticket {id: $ticket_id})
            CREATE (msg:TicketMessage {
                id: $msg_id, content: $content,
                author_id: $author_id, is_staff: false, created_at: $now
            })
            CREATE (msg)-[:IN]->(tk)
            SET tk.updated_at = $now, tk.status = 'open'
            """,
            ticket_id=ticket_id, msg_id=msg_id, content=body.content,
            author_id=current_user["id"], now=now,
        )

    return TicketMessageItem(
        id=msg_id, content=body.content, author_id=current_user["id"],
        author_name=current_user["name"], is_staff=False, created_at=now,
    )


@router.post("/tickets/{ticket_id}/close", response_model=TicketActionResponse)
def close_ticket(ticket_id: str, current_user: dict = Depends(get_current_user)):
    now = datetime.now(timezone.utc).isoformat()
    with get_session() as session:
        result = session.run(
            """
            MATCH (u:User {id: $user_id})-[:OWNS_TICKET]->(tk:Ticket {id: $ticket_id})
            SET tk.status = 'closed', tk.updated_at = $now
            RETURN tk.id AS id, tk.status AS status
            """,
            user_id=current_user["id"], ticket_id=ticket_id, now=now,
        ).single()
        if result is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found")
    return TicketActionResponse(message="Ticket closed", ticket_id=result["id"], status=result["status"])
