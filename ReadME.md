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

# Additional Problem

## Square substring problem
My understanding of the problem statement: Given a string, find the length of the shortest non-empty string w such that ww is a substring of the given string.

The output expected is \[length of the shortest square substring(s)\]\[earliest starting index of substring with that length\]

After struggling to wrap my head around the proposed solution within the problem for an hour, I asked an LLM to explain it to me.
What I took away from it:

- The solution substring will always have a length of 2w. here w is the length of the square string's repeating element.
- Start with the smallest possibility and keep iterating till the biggest one. w = 1,2,3 ... l = 1,2,4,6...
- Break the string into blocks of size l
-  l in range 1, to len(s)/2 in multiples of 2.
- This ensures that the smallest square string is found at the first occuring index.
- For every index i, check for same block upto 3l-2 indices far on the right.
- If a repeating character is found, check for the complete block of length w.
- For each (i,j) match, expand symmetically (left and right)
- If found, expand the square string w to left and right. If the length w = i - j Where the two blocks start then we have the smallest square.
That's the answer.

solution

```python
def solution(s: str) -> list[int]:
    n = len(s)
    l = 1
    ans = []
    while l <= n//2:
        i = 0
        j = l
        while j <= n-l:
            if s[i:i+l] == s[j:j+l]:
                # blocks meet the condition
                # There is a flaw in this - which assumes that blocks are of length l 
                break
            j += 1
        # Increment
        l *= 2
```

```python
def solution(s: str) -> list[int]:
    
    n = len(s)
    
    
    # Check for |w|=1 (a square like "aa")
    for i in range(n - 1):
        if s[i] == s[i+1]:
            return [1, i]

    # Check for |w|=2 (a square like "abab")
    for i in range(n - 3):
        if s[i:i+2] == s[i+2:i+4]:
            return [2, i]
            
    # --- General Algorithm for l = 2, 4, 8, ... ---
    l = 2
    while l <= n // 2:
        # In each phase, we find the best square (earliest start for the shortest length).
        phase_best_len = math.inf
        phase_best_pos = -1

        # Iterate through the string to find "witness" blocks of length l.
        for i in range(n - l + 1):
            block = s[i:i+l]
            
            # Search for the next occurrence of this block to its right.
            search_pos = i + 1
            while True:
                j = s.find(block, search_pos)
                if j == -1:
                    break

                # --- THE "EXTEND AND VERIFY" STEP ---
                # We found a matching core block at indices i and j.
                # Now, we extend outwards to find the full repeating string w.
                
                # 1. Extend backwards
                back_len = 0
                while (i - 1 - back_len >= 0 and j - 1 - back_len >= 0 and
                       s[i - 1 - back_len] == s[j - 1 - back_len]):
                    back_len += 1
                
                # 2. Extend forwards
                fwd_len = 0
                while (i + l + fwd_len < n and j + l + fwd_len < n and
                       s[i + l + fwd_len] == s[j + l + fwd_len]):
                    fwd_len += 1

                # 3. Verify if it's a valid square
                w_len = back_len + l + fwd_len
                distance = j - i
                
                if w_len == distance:
                    # It's a square! Check if it's the best one in this phase.
                    start_pos = i - back_len
                    if w_len < phase_best_len:
                        phase_best_len = w_len
                        phase_best_pos = start_pos
                    # If lengths are equal, keep the one with the smaller start index.
                    elif w_len == phase_best_len and start_pos < phase_best_pos:
                        phase_best_pos = start_pos
                
                # Continue searching from the character after the found match.
                search_pos = j + 1

        # If we found any square in this phase, it must be the shortest overall.
        if phase_best_pos != -1:
            return [phase_best_len, phase_best_pos]
            
        l *= 2 # Move to the next phase
        
    # If the loops complete without finding any squares.
    return [0, 0]

```