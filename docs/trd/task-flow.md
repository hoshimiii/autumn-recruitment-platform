# Task Flow

## Collection Run

1. Load enabled whitelist sources.
2. Skip manual-only, forbidden, login-required, captcha-required, and unknown compliance sources.
3. Run the matching adapter for each allowed source.
4. Preserve bounded candidate and parsed snapshot data.
5. Normalize candidate jobs.
6. Compute dedupe keys and change summaries.
7. Write new, changed, unknown-page, and low-confidence records to the review queue.
8. Record task result, timing, counts, and errors.

## Review Flow

1. User opens the review queue.
2. User compares candidate and normalized fields against source evidence.
3. User approves, rejects, or marks the item for manual follow-up.
4. The system writes an audit log for the action.
5. Approved new or updated records appear in the job table.

## Manual Action Flow

1. User creates a manual item for a restricted source.
2. The item records login, captcha, required action, reminder, application method, priority, and notes.
3. The system stores the item without invoking collection.
