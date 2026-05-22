monarch_cc — braille UI design doc
Overview
A minimal Claude Code status display for the APH Monarch refreshable braille display. Single screen, no navigation, no drill-down. Renders once on state change, never on a timer.

Display constraints

32 cells wide, 10 rows tall
8-dot braille hardware — real braille codepoints render correctly as cells
Output is plain UTF-8 written to a tty; BRLTTY handles the rest
No ANSI color codes, no box-drawing Unicode, no spinners


Layout — fixed 10-row grid
row 0   chrome header
row 1   current action  (tool + file, updates on tool change only)
row 2   separator
row 3   main task name + overall %
row 4   separator
row 5   subtask list header  (done/total count)
row 6   subtask slot
row 7   subtask slot
row 8   subtask slot
row 9   progress bar + elapsed
Rows 6–8 scroll if there are more than 3 subtasks. Side-button press while finger rests on a subtask row announces that row's detail to the screen reader; the display itself does not change.

Cell vocabulary
Only four braille codepoints carry meaning:
glyphmeaning⣿done / complete⠶error / failed⠂waiting>running (ASCII, universally readable)⠤separator line (repeated)
No spinners. A running job is > until it becomes ⣿ or ⠶. State, not animation.

Row specs
Row 0 — header
claude-code  agents:N active
Agent count updates on change. 32 chars, left-justified.
Row 1 — current action
> TOOL        FILE-OR-ARG
Updates only when the active tool call changes. If no tool is running, row 1 is a separator. Tool name left-padded to 10 chars, remainder is the file/arg truncated to fit.
Row 3 — main task
> TASK-NAME              PCT%
Icon + task name truncated to 24 chars, percent right-aligned in 4 chars. Icon is ⣿ when done, ⠶ on error.
Rows 6–8 — subtask slots
⣿ subtask-name          done
> subtask-name            >
⠂ subtask-name          wait
Icon + name truncated to 20 chars + 4-char status right-aligned. Waiting subtasks dim (render at lower contrast on capable displays, or just identical on hardware). Max 3 visible at once; scrolls by one row on repeated side-button presses at the bottom.
Row 9 — footer
5/8 ⣿⣿⣿⣿⣿⣿⣿⣿⠒⠒⠒⠒⠒⠒⠒⠒  47s
Done/total, braille fill progress bar (16 cells), elapsed seconds right-aligned. Bar uses ⣿ for filled, ⠒ for empty.

Update rule
Only write to the display when state changes. Renderer keeps a hash of the last-written state. If the hash matches, no write happens. This is the single most important rule — refreshable braille displays are tactile; a redraw resets the reader's finger position.
State that triggers a redraw:

Any subtask status transition
Tool call change (row 1)
Main task percent crossing a 5% threshold
Elapsed time — does not trigger redraws (omit live clock if it would cause constant redraws; update only when something else already triggers one)


Screen reader parallel output
The renderer maintains a second output path for screen readers alongside the braille display. They are separate concerns:

Braille display: compact, symbol-dense, 32 cells, written to tty on state change
Screen reader: plain English sentences, written to an aria-live region or spoken via a speech API on the same state change events

Announcements:

Subtask completes → "write_tests done in 18 seconds" (polite)
Subtask errors → "refactor_session failed — exit code 1" (assertive, interrupts)
Main task completes → "refactor auth module complete, 8 of 8 subtasks done" (assertive)
Interrupt/permission required → replaces row 3 entirely, announced assertive

Side-button press on a subtask row reads that row's detail aloud without touching the display.

Interrupt state
When Claude Code pauses for user input, the entire display switches to a single-purpose layout:
row 0   !! INPUT REQUIRED !!        
row 1   separator                   
row 2   agent: TASK-NAME            
row 3   (blank)                     
row 4   permission to run:          
row 5   TOOL  ARG                   
row 6   (blank)                     
row 7   (blank)                     
row 8   [y] allow    [n] deny       
row 9   all agents paused
Announced immediately as assertive. All other state frozen until resolved.

Python implementation shape
pythonclass MonarchDisplay:
def __init__(self, tty_path, width=32, rows=10):
self.tty = open(tty_path, 'w')
self.width = width
self.rows = rows
self._last_hash = None

    def update(self, state: AppState):
        rows = render(state, self.width, self.rows)
        h = hashlib.md5('\n'.join(rows).encode()).hexdigest()
        if h == self._last_hash:
            return                      # nothing changed, do not write
        self._last_hash = h
        self.tty.write('\033[H' + '\n'.join(rows))
        self.tty.flush()
render() is a pure function: takes state, returns a list of exactly rows strings each exactly width chars. No side effects. Easy to unit test by asserting on the string list directly.

What is explicitly out of scope

Spinners or any animation requiring timer-driven redraws
Color or brightness variation (hardware is monochrome)
Drill-down / detail views (single screen only)
Progress percentages updating more than every 5%
Any Unicode outside the four braille codepoints listed above plus printable ASCII