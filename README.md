# MonarchBlocks

An Android sample app for the [APH Monarch](https://www.aph.org/monarch/) refreshable braille display. It demonstrates how to drive the Monarch's braille surface using the HumanWare KeySoft SDK, and includes a live Claude Code integration that lets blind users query Claude directly from the display.

## Display specs

| Property | Value |
|---|---|
| Braille cells | 32 columns × 10 rows |
| Pin grid | 96 × 40 pins |
| Cell size | 3 × 4 pins |

## Activities

### Claude Code Demo (`ClaudeCodeDemoActivity`)

![Claude Code demo on the Monarch braille display](MonarchBlocks/Screenshot%202026-05-22%20at%202.10.25%E2%80%AFPM.png)

A self-running presentation that shows a simulated Claude Code agentic task progressing through four stages (0% → 25% → 61% → 100%). Highlights:

- **Animated braille spinners** on every running subtask, drawn directly to the `DotsMatrix` at the right edge of the display (pin x=93, braille column 31) so they survive the text-to-braille translation pipeline unchanged.
- **Looping** — cycles back to frame 1 after completion, advancing every 3.5 s.
- **Manual navigation** — PAGE_DOWN / PAGE_UP step through frames; MOVE_HOME resets to frame 1.
- **Readable text overlay** rendered on top of the `SelfBraillingWidget` for sighted viewers watching on a phone or emulator screen.

### Claude Code Chat (`ClaudeCodeActivity`)

A live chat interface backed by the Anthropic Messages API (`claude-sonnet-4-6`). Flow:

1. **API key entry** — prompted on first launch; stored in `SharedPreferences`.
2. **Prompt input** — full-screen `EditText`; submit with Enter (Dots 8).
3. **Loading** — blank braille screen while the request is in flight; TTS announces "Thinking".
4. **Response** — scrollable braille output via `BrlScrollView`.

Response navigation keys:

| Key | Action |
|---|---|
| PAGE_DOWN / PAGE_UP | Next / previous page |
| MOVE_HOME / MOVE_END | First / last page |
| ZOOM_IN / ZOOM_OUT | Increase / decrease line spacing |
| MENU | Return to prompt input |

### Other samples

The app also ships examples of `SelfBraillingWidget` usage, braille translation, dialog patterns, and custom graphics — accessible from the main menu.

## Project structure

```
MonarchBlocks/
├── app/src/main/java/org/aph/monarchblocks/
│   ├── activities/
│   │   ├── ClaudeCodeActivity.kt        # Live Claude chat
│   │   ├── ClaudeCodeDemoActivity.kt    # Animated demo
│   │   └── ...                          # Other sample activities
│   ├── contextmenu/                     # Context menu examples
│   ├── monarch_utils/
│   │   ├── BrailleUtil.kt              # Text ↔ braille translation helpers
│   │   ├── BrlFormatter.kt             # Braille line wrapping (32-col)
│   │   ├── BrlScrollView.kt            # Paginated braille scroll view
│   │   ├── Drawing.kt                  # Low-level DotsMatrix rendering
│   │   └── ...
│   └── Constants.kt                    # SCREEN_WIDTH=96, SCREEN_HEIGHT=40
└── monarch-docs/                        # Hardware interface documentation
```

## Building

Open in Android Studio and sync Gradle. The app targets the Monarch device; the HumanWare KeySoft SDK AAR is required and configured in the project's local Maven repository.

## License

MIT — see [LICENSE](LICENSE).
