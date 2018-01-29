# xref id camel

A simple cross-referencing service for storing ids across different systems.

```$json
{
  "id": 5,
  "commonID": "ace664a0-1d28-412a-9ea5-0bbf5739f4ce",
  "references": [
    {
      "id": 7,
      "endpoint": "idm",
      "endpointId": "test.user"
    },
    {
      "id": 8,
      "endpoint": "doie",
      "endpointId": "22311415"
    }
  ]
}
```

## Sample Two-system interaction

### Create entity in a new system

![Create Entity](puml/xref-sequence-create.png "Create")

### Update or interact with a different system, looking up the id

![Interact Entity](puml/xref-sequence-aftercreate.png "Interact")

### Remove a relation

When a system no longer has an entity, the reference to that entity in the XRef service should be removed.

![Remove Relation](puml/xref-sequence-relationremoval.png "Remove")


### TODO:

- Documentation: Create sample flow diagrams
- Documentation: Explain flow
- JSONView for the Jackson output (no db ids)
- Run on Openshift (template)
- Create H2 version to run unit tests against.
- Create sample scenario in unit test
