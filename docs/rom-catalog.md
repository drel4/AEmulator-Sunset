# Sunset ROM catalog

Open **ROM catalog** using the list icon on the library screen or its row in app
settings. The pencil button changes the `rom.list` URL; the default is
`https://dumpster.ralsei.tech/drel/AESSRomCatalog/rom.list`. The address is saved
per app variant. Refresh reloads the catalog. HTTP sources are supported, but
HTTPS is recommended; HTTPS redirects cannot silently downgrade to HTTP.

UTF-8 format (LF or CRLF, optional BOM):

```
#Message of the day shown at the top
[Section name];Optional comment;https://example.org/source
Device name;Android version;Skin;1;Optional status comment;https://example.org/rom.zip
[Empty section];;
Device without link;4.4.2;Xperia UI;-1;;
```

Status: `-1` unknown, `0` does not boot, `1` boots with issues, `2` works.
Statuses are reports from the catalog, not compatibility checks performed by
the app. Multiple leading `#` lines form the MOTD. Later `#` lines are comments.
Blank fields retain their position. Fields are not CSV-quoted; use semicolons
only as separators, except that the final URL can itself contain semicolons.

Empty sections remain visible with **No ROMs at the moment.** Missing download
URLs show **ROM download URL isn't available right now.** Invalid non-web URLs
are not opened. Malformed records are reported by line number without hiding
valid entries. Downloads and section/source links open in the default browser,
not in an embedded web view or through an automatic importer. If no default
browser is set, Android offers browser selection.

The list fetch is bounded to 1 MiB, 10000 lines, five redirects and socket
timeouts. A failed refresh leaves the last successfully displayed list visible
with an error; changing the source clears the old list. There is no persistent
offline catalog cache.

## Experimental unlock

Tap the **AEmulator Sunset** title 42 times on the library screen or app-settings
about card. Both titles share a counter that survives rotation and app restarts. At tap 42, the app saves the
experimental-feature unlock, shows a translated toast, and opens
`https://www.youtube.com/watch?v=LVHBcsiZcyI` in the browser. Further taps do not
reopen the video. Unlocking does not turn experimental features on by itself.

Currently this reveals the **Host camera (experimental)** setting. A ROM with
the camera already enabled keeps its setting available; updating does not
silently disable existing camera configurations. The catalog, setup-skip
option, vibration, navigation controls and ordinary ROM support are not gated.
