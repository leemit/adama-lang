---
id: reference-defining-functions-and-procedures
title: Defining Functions & Procedures
---

## Fast Intro

Adama has two forms of colliqual function.


Functions in Adama are pure in that they have no side-effects and also are context-free.
```adama
function square(int x) -> int {
  return x * x;
}
```

```adama
int x;
procedure square_of_x() -> int {
  return x * x;
}
```

## Diving Into Details