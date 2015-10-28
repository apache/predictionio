# Removing old events from app

## Documentation

This shows how to remove old events from the certain app.

Parameters in engine.json are appId and cutoffTime.
All events in that appId before the cutoffTime are removed,
including $set, $unset and $delete
(so please adapt it for use when you want to preserve these special events).

To use, edit `engine.json`, run `pio build` then `pio train`.
