// Ensure consistent structure by touching defaults
MATCH (t:Ticket)
SET t.created_at = coalesce(t.created_at, datetime().toString()),
    t.updated_at = coalesce(t.updated_at, datetime().toString()),
    t.status = coalesce(t.status, 'open'),
    t.priority = coalesce(t.priority, 'normal');