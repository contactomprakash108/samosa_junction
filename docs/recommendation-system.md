# Recommendation system — Phase 14


`GET /api/recommendations` scores the **current catalog** against a taste profile derived from the caller’s **paid orders**. There is no recommendation table, no LLM, and no Python process.

## Why content-based

The seeded catalog is seven named snacks that are all “samosa.” Collaborative filtering needs many users overlapping on the same items. We do not have that. Category, ingredients, calories, protein, spice, and dietary tags *do* exist on every product. Those fields are enough to say “you paid for paneer / high protein / mild, so try High Protein Samosa.”

Collaborative filtering (users who bought X also bought Y) is a later option once there is real overlap. It is not faked here.

## Feature tokens

Product names are not tokens. Every name contains “samosa” and would collapse the space.

| Prefix | Source | Example |
| --- | --- | --- |
| `cat:` | category code | `cat:PROTEIN` |
| `ing:` | ingredient, lowercased | `ing:paneer` |
| `tag:` | dietary tag, uppercased | `tag:HIGH_PROTEIN` |
| `spice:` | `SpiceLevel` | `spice:MILD` |
| `cal:` | calorie band | `cal:HIGH` |
| `pro:` | protein band | `pro:HIGH` |

Bands (chosen so the seed catalog actually splits):

- calories: `< 220` LOW, `< 300` MED, else HIGH
- protein grams: `< 8` LOW, `< 12` MED, else HIGH

## Scoring

1. Load the catalog (category join + batch ingredients/tags). Fit **smoothed TF-IDF** on those bags. IDF is `ln((1+N)/(1+df)) + 1`.
2. Build the user vector as the **quantity-weighted sum** of TF-IDF vectors for products on paid orders.
3. Rank **available** products by **cosine similarity**. Tie-break by name. Score is rounded to 4 decimals.
4. Prefer products the user has **not** already ordered. If that set is smaller than `limit`, already-ordered available items are filled back in.
5. Unavailable products are never returned.

`limit` is 1–10, default 5.

## Paid vs ignored orders

Counted: `CONFIRMED`, `PREPARING`, `READY`, `OUT_FOR_DELIVERY`, `DELIVERED`.

Ignored: `CREATED` (unpaid) and `CANCELLED`. Quantity on the line is the weight.

The profile is computed **on read** from `orders` / `order_items`. Kafka `samosa-recommendation` still only logs. We did not event-source a preference table; seven products and a few orders do not need one.

## Cold start

No paid orders (or every paid product has been deleted from the catalog): `coldStart=true`, `tasteTokens=[]`, scores are `0`. Sort is protein descending, then name. That is an honest catalog listing, not fake popularity.

## Explainability

Each item has `reason` (shared tokens, or a cold-start / low-overlap sentence). `tasteTokens` are the top profile keys by weight.

## Why nothing is persisted

Recommendations are a **query**, like search. Storing rows would go stale the moment staff change a product or a customer pays. Metadata stays on `products`. Image URLs are the same presigned links as `GET /api/products`.

## What this is not

- Not an LLM. Not collaborative filtering. Not “customers also bought.”
- Not a claim about scale. TF-IDF is in-memory over the catalog on each request.
- Not a Python `ai-service`. Phase 15 is the ordering agent; it can call this API later.
