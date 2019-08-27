USE tax;

INSERT INTO contributors JSON '{
 "id": "001-001",
 "region": "001",
 "registration_date": "2018-08-19T14:20:38Z",
 "yearly_income": 12000.50,
 "yearly_contribution": 6000.50
}' IF NOT EXISTS;

use tax;

INSERT INTO contributions (contributor_id, year, month, income, contributions) VALUES  (
 '001-001', 2018, 08, 1500.0,
 [ {type: 'MAL2', income: 1500, base_income: 1400, rate: 6.5, contribution: 40.50}]
 
);

INSERT INTO contributions JSON '{
 "contributor_id": "001-001",
 "year": 2018,
 "month": 9,
 "income": 1500,
 "contributions": [
    {
      "type": "MAL2", 
      "income": 1500.00, 
      "base_income": 1450.00, 
      "rate": 6.5, 
      "contribution": 40.25 
    }
    ]
}';

INSERT INTO contributions JSON '{
 "contributor_id": "001-001",
 "year": 2018,
 "month": 7,
 "income": 2000,
 "contributions": [
    {
      "type": "MAL1", 
      "income": 1500.00, 
      "base_income": 1450.00, 
      "rate": 6.5, 
      "contribution": 40.25 
    },
    {
      "type": "MAL2", 
      "income": 1500.00, 
      "base_income": 1450.00, 
      "rate": 6.5, 
      "contribution": 40.25 
    }
    ]
}';