CREATE INDEX ticket_status IF NOT EXISTS
FOR (t:Ticket)
ON (t.status);

CREATE INDEX ticket_updated IF NOT EXISTS
FOR (t:Ticket)
ON (t.updated_at);