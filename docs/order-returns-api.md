# Order returns API

All routes require authentication (HTTP Basic or the login session) and the `ADMIN` role.
Authorization uses the role, not a username such as `admin`.
Anonymous requests return 401; authenticated users without the role return 403.
List and detail responses use the existing `BaseResponse` wrapper.
The export returns a CSV attachment directly.

## List returns

`GET /api/orders/returns?page=0&size=4&filterBy=all%20requests`

`page` is zero-based and defaults to 0.
`size` defaults to 4 and must be between 1 and 100.
`filterBy` defaults to `all requests`.
Supported filters are `all requests`, `PENDING`, `IN_TRANSIT`, `WAREHOUSE_RECEIVED`, `RESTOCKED`, and `REFUNDED`, matching the existing filter contract.
Filters ignore case and accept spaces or hyphens instead of underscores.
`Allrequest` and `Allrequests` are also accepted as aliases for `all requests`.
Other filters and invalid pagination return 400.

`data` contains the page's `content`, `totalElements`, `totalPages`, `number`, and `size`.
Rows sort by creation time descending, then ID descending.
Deleted returns are excluded.
An out-of-range page returns empty content while preserving the total count.

## Return detail

`GET /api/orders/returns/{id}`

`id` is the return ID from the list response, not the order ID or return code.
Missing and deleted returns return 404 with `Order return not found`.
`data` contains the return header and an `items` array.
Each item includes `orderItemId`, `productVariantId`, `productName`, `reasonType`, `quantity`, `conditionStatus`, `unitPrice`, and `refundAmount`.
Prices and refund amounts come from the return item, not the current product price.
Deleted return items are excluded; a return with no active items has an empty array.

## Shared header

Both responses include `returnId`, `initialTime`, `customerName`, `reasonReturn`, `originType`, and `orderReturnStatus`.
`initialTime` is the elapsed whole minutes since creation, with missing or future creation times reported as zero.
`reasonReturn` combines distinct reason types from active return items in creation order, separated by commas.
It is an empty string when there are no active items.

## Export returns

`GET /api/orders/return/export?filterBy=PENDING`

Connect the Export to CSV button to this URL using the existing authentication.
The response is `text/csv;charset=UTF-8` with `Content-Disposition: attachment; filename=order-returns.csv`.
For a fetch/Axios request, read the response as a Blob and download it; do not parse it as JSON.
`filterBy` accepts `ALL_REQUESTS`, `PENDING`, `IN_TRANSIT`, `WAREHOUSE_RECEIVED`, `RESTOCKED`, and `REFUNDED`.
Omit `filterBy` or pass `ALL_REQUESTS` to export all active returns.
Pass a status to write only matching returns to the CSV file.
The export includes every matching active return, regardless of the current UI page.
Unsupported filters return 400; anonymous and non-admin requests return 401 and 403.

The flat `ExportOrderReturn` DTO exports `returnId`, `initialTime`, `customerName`, `reasonReturn`, `originType`, and `orderReturnStatus`.
`initialTime` is measured in whole minutes at export start.
Empty results still produce the CSV header.
EasyExcel writes UTF-8 with a BOM and handles commas, quotes, and line breaks.
Text values are exported unchanged, without adding an apostrophe prefix.

The repository fetches 500 projected rows at a time using only `cursorId`, ordered by ID descending, without a total-count query or increasing offsets.
Each subsequent batch selects IDs less than the last ID from the previous batch.
Export order follows UUID values; it is not chronological.
Reasons are fetched as scalar values for each batch, so exported entities do not accumulate in the persistence context.
The service writes the temporary file; the controller copies the completed file to the response and deletes it even if the client disconnects.
Generation failures also delete the partial file.
This is a synchronous download, so request/proxy timeouts and temporary disk space must accommodate the export size.
Apply `docs/sql/order-return-export-indexes.sql` once to the existing database before large exports; the application uses schema validation and does not create these indexes automatically.

Run the focused tests with JDK 25:

```sh
./mvnw -Dtest=OrderReturnsIntegrationTest,OrderReturnFilterTest test
```
