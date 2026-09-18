# Knight's Tour Pro

## Commit messages

Write commits in **English**, in plain language. Someone skimming `git log` should
understand what changed without opening the diff and without knowing the codebase.

**Subject line**
- One line, imperative mood, 72 characters or fewer.
- No `feat:` / `fix:` / `chore:` prefixes, no ticket numbers, no scope brackets.
- Say what changed in ordinary words. Class and method names belong in the subject
  only when they *are* the point of the change.

**Body** (optional)
- Add one only when the reason is not obvious from the subject.
- Two or three sentences explaining *why*, not *how*. The diff already shows the how.
- Do not list every file touched.

**Examples**

Good:
```
Load the knight image from the right resource name

The board was looking for knight.png but the file is knight2.png, so the
GUI always fell back to the text glyph.
```
```
Fix the gitignore patterns so build output stays out of the repo
```
```
Let CLI flags work without an explicit strategy argument
```

Too technical / too verbose:
```
fix(ui): correct KNIGHT_RESOURCE constant in BoardView.java:37 from
"/knight.png" to "/knight2.png" so that loadKnightImage() returns a non-null
Image and the ImageView branch of the Cell constructor is taken instead of
the knightGlyph fallback branch
```

Too vague (loses information):
```
fix bug
```
