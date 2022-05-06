package parsing

object TestUtils {
  val formResourceFieldDescriptor =
    """
    [
        {
            "cardinality": "single",
            "field": {
                "society": {
                    "name": "string"
                }
            }
        },
        {
            "cardinality": "single",
            "field": {
                "userInfo": {
                    "firstName": "string"
                }
            }
        },
        {
            "cardinality": "single",
            "field": {
                "userInfo": {
                    "lastName": "string"
                }
            }
        },
        {
            "cardinality": "single",
            "field": {
                "userInfo": {
                    "email": "string"
                }
            }
        },
        {
            "cardinality": "single",
            "field": {
                "date": "date"
            }
        },
        {
            "cardinality": "single",
            "field": {
                "company_name": "string"
            }
        },
        {
            "cardinality": "single",
            "field": {
                "company_contact_name": "string"
            }
        },
        {
            "cardinality": "single",
            "field": {
                "company_contact_email": "string"
            }
        },
        {
            "cardinality": "single",
            "field": {
                "company_contact_phone_number": "number"
            }
        },
        {
            "cardinality": "single",
            "field": {
                "acc_number": "number"
            }
        },
        {
            "cardinality": "single",
            "field": {
                "sort_code": "string"
            }
        },
        {
            "cardinality": "loop",
            "field": {
                "items": [
                    {
                        "cardinality": "single",
                        "field": {
                            "date": "date"
                        }
                    },
                    {
                        "cardinality": "single",
                        "field": {
                            "description": "string"
                        }
                    },
                    {
                        "cardinality": "single",
                        "field": {
                            "cost": "string"
                        }
                    }
                ]
            }
        },
        {
            "cardinality": "single",
            "field": {
                "group_account_to_debit": "string"
            }
        },
        {
            "cardinality": "single",
            "field": {
                "first_signature": "string"
            }
        },
        {
            "cardinality": "single",
            "field": {
                "first_signature_position": "string"
            }
        },
        {
            "cardinality": "single",
            "field": {
                "second_signature": "string"
            }
        },
        {
            "cardinality": "single",
            "field": {
                "second_signature_position": "string"
            }
        },
        {
            "cardinality": "single",
            "field": {
                "admin_signature": "string"
            }
        },
        {
            "cardinality": "single",
            "field": {
                "admin_position": "string"
            }
        }
    ]
        """
}
