# API Handoff

This documents the existing contract consumed by the Android client, not a newly implemented
backend. Endpoint paths and JSON key casing below are significant. All API calls are GET.
Configure the base URL with `SDS_BASE_URL` at build time; the default is `https://sds.par-crm.com/`.

## Identification

`GET Display/GetMenuType?u=<device-id>&c=<screen-code>`

```json
{"Status": true, "is_menu": "1"}
```

`is_menu: "0"` selects web display. `"1"` selects image/video content. Other values are rejected.
The screen code is uppercase and matches `[A-Z0-9_-]{1,64}`. The client persists a device ID
and preserves the prior installed version's ID when upgrading. Clearing a screen session
does not reset that ID. Treat it as an identifier, not an authentication credential.

## Playlist

`GET Display/Menu?u=<device-id>&c=<screen-code>`

```json
{
  "Status": true,
  "page_detail": [{"page_code": "VIDEO_PAGE", "template_id": "4"}],
  "channel_data": {"refresh_rate": "5000"}
}
```

The client supports video-only playlists (template `"4"`) or image-only playlists (templates
`"2"` and/or `"3"`). It preserves `page_detail` order. Mixed video/image or unsupported templates
are rejected. Changing a configured screen's mode requires operator re-identification;
the old cached playlist continues until then.

`refresh_rate` controls slideshow page duration, not API polling. Prefer milliseconds
(`"5000"` = 5 seconds, `"300000"` = 5 minutes). For compatibility, positive values below 1000
are treated as seconds. Invalid/nonpositive values default to 5 seconds, with a maximum of
24 hours. Make this unit convention explicit in the future API.

## Page Media

`GET Display/GetPage?u=<device-id>&c=<page-code>`

Note that `c` is the **page code** here, not necessarily the screen identifier.

Video page (template 4), in playback order:

```json
{
  "Status": true,
  "page_data": [
    {"filename": "intro-v1.mp4"},
    {"filename": "https://media.example.com/promo-v2.mp4"}
  ]
}
```

Single-image page (template 2): each entry is one slideshow page.

```json
{"Status": true, "page_data": [{"filename": "poster-v1.png"}]}
```

Three-panel page (template 3): exactly one image per slot; slot 1 is left, 2 is top, 3 is right.

```json
{
  "Status": true,
  "page_data": [
    {"filename": "left-v1.png", "slot": "1"},
    {"filename": "header-v1.png", "slot": "2"},
    {"filename": "right-v1.png", "slot": "3"}
  ]
}
```

Relative filenames resolve beneath `<base>/assets/contents/`; an existing `assets/contents/`
prefix is not duplicated. Absolute HTTPS URLs can use a separate media host. Do not return
local-file URLs, URL credentials, traversal segments, empty paths, or protocol-relative URLs.
For encoded paths or signed query strings, prefer a full HTTPS URL.

### Download and Cache Contract

- Return successful HTTP responses with actual media bytes, accurate `Content-Length` when
  present, and appropriate image/video content types, not an HTML sign-in page or JSON error.
- Use media formats supported by the target Android hardware. The client does not transcode.
- **Media URLs must be immutable/versioned.** A nonempty file already cached for an identical
  URL is reused. To change the bytes, change the filename/version or URL query string.
- The client downloads every required asset before atomically committing the new manifest.
  Only after that commit are obsolete cached files deleted.
- An HTTP failure, `Status: false`, missing page/media, incomplete download, or unsupported
  template retains the previous complete playlist. An empty playlist is not a remote erase command.
- HTTP cleartext is rejected, except explicit loopback hosts in debug builds. HTTPS-to-HTTP
  redirects are disabled. All production endpoints and media must have valid TLS certificates.

## Web Display

`GET display?c=<screen-code>&u=<device-id>` returns the rendered HTML display.

JavaScript and DOM storage are enabled. Top-level navigation is restricted to the configured
base URL's exact scheme, hostname, and port. Local file/content access, geolocation, mixed
content, and third-party cookies are disabled. External login redirects are not supported by
this flow. Serve required web resources over HTTPS; keep display navigation on the same origin.

## Errors

Return a suitable non-2xx status for HTTP failures. API responses may also use:

```json
{"Status": false, "Message": "This screen has no assigned content."}
```

The optional `Message` is operator-facing; keep it short and omit stack traces, credentials,
or private server details. Null/empty bodies are handled as failures rather than valid playlists.

## Separate Backend Decisions

No API token or user authentication has been invented in the client. Screen codes and `u`
alone do not establish authorization. Define enrollment/authorization, revocation, and media
access before exposing private content, then integrate that agreed mechanism separately.

The invite feature shares the screen code, not credentials. Its telemetry is local only;
server-side fleet monitoring and cross-device referral attribution are not implemented.
