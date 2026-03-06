# slinq-zio-http-demo

A comprehensive demonstration REST API showcasing [Slinq](https://github.com/karimagnusson/slinq), a type-safe SQL query builder for PostgreSQL, integrated with [ZIO HTTP](https://github.com/zio/zio-http) for building high-performance web services.

## Tech Stack

- **Scala** 3.3.7
- **ZIO** 2.1.22
- **ZIO HTTP** 3.7.0
- **Slinq** 0.9.6-RC2
- **PostgreSQL** (World database sample)

## Features

This demo provides practical examples of:

### Core Operations

- **CRUD Operations** - Type-safe SELECT, INSERT, UPDATE, DELETE queries
- **Type-Safe Queries** - Compile-time verified SQL with case class mapping using zio-json
- **Query Caching** - Pre-compiled queries with dynamic WHERE conditions for improved performance

### Advanced Features

- **Streaming** - Efficient CSV export/import with streaming data processing
- **JSONB Support** - PostgreSQL JSONB field operations (query, update, nested access)
- **Array Operations** - PostgreSQL array field manipulation
- **Date/Time Functions** - PostgreSQL timestamp methods and operations
- **JOIN Queries** - Multi-table queries with subqueries and aggregates
- **Conditional WHERE** - Optional query parameters with type-safe builders

### Code Examples

Each route file demonstrates specific functionality:

- `SelectRoute.scala` - SELECT queries with JOINs, subqueries, and aggregates
- `OperationRoute.scala` - INSERT, UPDATE, DELETE with RETURNING
- `TypeRoute.scala` - Type-safe queries with case class serialization
- `CacheRoute.scala` - Cached queries with pickWhere
- `StreamRoute.scala` - Streaming CSV export/import
- `JsonbRoute.scala` - JSONB field operations
- `ArrayRoute.scala` - PostgreSQL array operations
- `DateRoute.scala` - Timestamp and date functions

## Getting Started

### Prerequisites

- PostgreSQL installed and running
- Scala and sbt installed

### Database Setup

1. Create the database:

```sql
CREATE DATABASE world;
```

2. Import the sample data:

```bash
psql world < db/world.pg
```

### Configuration

Update the database credentials in `src/main/resources/application.conf`:

```conf
db {
  name = "world"
  user = "<YOUR_USERNAME>"
  pwd = "<YOUR_PASSWORD>"
}
```

### Running the Application

Start the server on port 9000:

```bash
sbt run
```

Stop the server with `Ctrl+C`.

### Testing with Postman

If you use [Postman](https://www.postman.com/), import the collection from `postman/demo.json` to get all endpoints pre-configured with example requests.

## API Examples

The demo includes various endpoints demonstrating Slinq features:

- `GET /select/country/:code` - Simple SELECT query
- `GET /select/cities/:code` - JOIN with custom field names
- `POST /type/insert/trip` - Type-safe INSERT with case class
- `GET /stream/export/:coin` - Stream database results as CSV
- `POST /stream/import` - Stream CSV file to database
- `GET /jsonb/country/:code` - Query JSONB fields
- `PATCH /array/add/lang` - Add element to PostgreSQL array

See the route files in `src/main/scala/routes/` for complete implementation details.

## Learn More

- [Slinq Documentation](https://slinq.kotturinn.com/)
- [Slinq GitHub](https://github.com/karimagnusson/slinq)
- [ZIO HTTP Documentation](https://zio.dev/zio-http/)
- [ZIO Documentation](https://zio.dev/)
