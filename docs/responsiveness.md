# Split-pane layout checks

Arcade Home switches to a compact title, credit action and game cards in narrow
or short panes. Game costs, credit requests and the Battleship waiting count stay
available. Home retains wrapping cards and a scrolling page, with a visible
scrollbar.

2048 places its controls beside the board in wide, short panes. In narrow panes
it wraps its header/actions and scrolls when there is insufficient height for a
readable board. Board geometry comes from its measured constraints. Below 200dp
of board width, the screen asks for a larger pane and ignores gameplay keys;
Back remains reachable. This does not promise that an entire board and every
control fit simultaneously in a 240dp-high pane.

Typing Sprint separates its title from wrapping statistics and scrolls its
passage, results and actions. Its input and view model are retained during pane
resizing. Leaderboard overlays also scroll when their contents exceed the pane.

Run the real Compose screen checks without starting BOSS:

```sh
./gradlew test --tests '*ResponsiveScreensTest'
```

The fixtures use fake credits and absent host services; they make no network
requests and do not access the user's game storage or audio devices. Screenshots
are written to `build/responsive-screenshots/`.

The logical-dp viewport matrix is 1000×700, 600×500, 420×300, 300×500, 900×240
and 300×240, plus the 600×400 side-by-side boundary and 2048's 180×160 recovery state. Tests check measured board/cell
bounds, reachable actions, resize state retention, credit-charge stability and
Typing Sprint input after resizing. Reachability checks explicitly scroll;
passing them is not evidence that all controls are visible without scrolling.

Before release, verify the installed plugin in vertical, horizontal and nested
splits, and in separately resized windows. Test moving a running game between
panes, overlays, density/font changes and restoring a saved layout. The fixture
does not exercise BOSS window management or embedded browser interaction.

Other Arcade game screens, credit dialogs and the host's other tab/plugin types
remain separate audit work. Do not interpret this bounded fix as a guarantee for
all Arcade screens or all BOSS tabs.
