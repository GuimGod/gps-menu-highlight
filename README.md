# GPS Menu Highlight

When your GPS (or Shortest Path) route uses a teleport that is picked from a menu, this
plugin colours the destination to pick:

- **Portal Nexus** - colours the row and scrolls the list to it.
- **House jewellery box** - colours the row.
- **Item right-click menu** (max cape, diary cape and so on) - colours the destination and
  the option that opens its sub-menu.

Known limit: with GPS, a teleport from a charged item (amulet of glory, games necklace...)
is not highlighted, because GPS does not publish that step under its default item mode.

## Setup

In the GPS (or Shortest Path) plugin settings, turn on **Post transports** (Debug section).
That option makes the pathfinder publish the steps of the route it is showing; without it
this plugin has nothing to read.

## Settings

- **Portal Nexus**, **Jewellery box**, **Item right-click menus** - turn each highlight on or off.
- **Highlight colour**.
- **Scroll to it** - scroll the list to the destination once, when the menu opens.
- **Forget route after** - the pathfinder does not announce a cleared route, so a route older
  than this is ignored.
- **Diagnostic logging** - writes the route steps and menu rows to the client log.

## How it works

The mark-and-scroll step follows the fairy ring log helper of the GPS plugin
(https://github.com/PauloAguiar/runelite-gps-plugin, BSD 2-Clause): find the row by its text,
recolour it, set the scroll position, run the scrollbar update script.

## What it never does

It only changes the colour of one row's text and the scroll position of the list. It does
not click, select, hide, reorder or add anything, does not change click areas or shortcut
keys, and does no network or file access.

## License

BSD 2-Clause, see `LICENSE`.
