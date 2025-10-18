# Simple Interest Calculator 

## Objective
I have to create a spring-boot server with tomcat embedded in it. This server tracks loan accounts and calculates interest.

## Notes and assumptions from reading material online
- Charge interest only for the period the funds are actually outstanding. *Interest must start from actual disbursal date and be day accurate*.
- Apply interest at monthly rests (i.e. monthly compounding.) (Maybe have a provision to add daily interest for special cases)
- Penal charges are not interest, must not be capitalized, and must be levied only on the amount under default.

So, 
- The daily interest calculator will accumulate the interest everyday at 24 hour intervals. But not add it to the principal (yet).
- At month end, add accumulated interest to principal. Next months daily calculations use NEW principal (which includes last month's interest).
- Any penalties ex. late fees should be calculated and handled separately (not added to the principal). It is included to the total due.

## Reccurring jobs

### Daily Job
- Calculates the interest based on the principal amount every day at 11:59:00 PM IST (Bec India).
### Monthly Job
- Adds the interest to the principal and clears the accumulated interest.

## Technical notes and thoughts:
- For the daily and monthly jobs, batch processing and locking would be useful for scaling up.
- To make this code modular, I am writing a dockerfile and a docker-compose YAML.