# Introduction #
This post is focused on RDMBS query performance for a simple database schema. The schema is just 1:N relationship, modeled using foreign key (see image1). This specific example uses person and address tables. Address table contains person\_fk foreign key, without an index on this column. The business requirement for a query on this schema is: find all records from Person table such that they have at least a specified set of addresses. The set of addresses is a set of database identifiers of those addresses (it doesn't really matter). This input set may vary in size.
![schema](https://github.com/nguyenfilip/otm/blob/master/pics/schema.png)

## Query Strategies ##
The business requirement can be solved using several, very different, queries. The three strategies considered are: 
  * Usage of INTERSECT operator
  * Correlated Exists Subqueries
  * Group by with Having clause
Now, lets explain the three strategies one by one. 
The most intuitive is INTERSECT. The idea is that for each address you will construct a select that will search for people with that address. For example searching for people that have set of addresses {126, 127} the query looks like this: 
```sql
 SELECT * 
 FROM person p LEFT JOIN address a on p.id=a.person_fk
 WHERE a.id=126

INTERSECT

 SELECT * 
 FROM person p LEFT JOIN address a on p.id=a.person_fk
 WHERE a.id=127
```
The second strategy correlated exists subqueries is using 1 subquery for each address. The idea is to use EXISTS(q) SQL function. This function returns true if and only if the parameter q is a query that returns some rows. Again searching for people with {126, 127} would look like this: 
```sql
 SELECT * FROM person p 
 WHERE 
   EXISTS ( SELECT * FROM address a WHERE a.person_fk = p.id AND a.id=126 ) AND
   EXISTS ( SELECT * FROM address a WHERE a.person_fk = p.id AND a.id=127 )
```
The third strategy is more tricky. Assume you have 3 people with a few addresses. After you create a JOIN you will end up with something like this:

 Person Name   | Address Name  |       
------------- |---------------| 
Filip         | Prague        | 
Filip         | Brno          | 
Filip         | London        | 
Matej         | Prague        | 
Matej         | London        | 
Marek         | London        | 
Marek         | Brno          | 

If we want to search for {Prague, London} then what we can do is to filter out (using OR) only those that have at least 1 of these addresses: 

Person Name   | Address Name  |       
------------- |---------------| 
Filip         | Prague        | 
Filip         | London        | 
Matej         | Prague        | 
Matej         | London        | 
Marek         | London        | 

If we now group by this result by person and  count the addresses we will have aggregation like this:

Person Name   | Address count  |       
------------- |----------------| 
Filip         | 2              | 
Matej         | 2              | 
Marek         | 1              | 

So now if we just filter out (using HAVING) those that have at least the size of the input set (2 cities) we correctly end up with Filip and Matej. The SQL query looks like this: 
```sql
 SELECT p.name, count(*) FROM person p LEFT JOIN address a ON p.id = a.person_id
 WHERE a.name = 'Prague' OR a.name='London'
 GROUP BY p.name
 HAVING count(*) = 2
```


# Experiment #
I run the three query strategies against PostgreSQL database. The schema consists of 2 tables person and address: 

```sql
  CREATE TABLE person(id SERIAL PRIMARY KEY, name varchar(40))
  CREATE TABLE address(id SERIAL PRIMARY KEY, person_id integer REFERENCES person(id), street varchar(40))
```
Person table is populated with 20000 records and for each person record there is randomized number of addresses in address table (100-200 records).

I have run EXPLAIN ANALYZE on the database server for different input lenghts. Input length is a lenght of a set of addresses that we search for. So for example if the input length is 5 it means that each of the three queries is used to search people that have a specific set 5 addresses. Obviously increasing the size of this set will also increase query times.

![20k](https://github.com/nguyenfilip/otm/blob/master/graphs/20k.png)

The graph shows that exists strategy and intersect are fairly close with this dataset size. Note we have not used a foreign key on the person\_fk column. The interesting thing here is that adding that index doesn't seem to change the results significantly. Another interesting thing is that with smaller dataset size (1000 records in person table) the EXISTS strategy has significantly better performance than EXISTS subquery.


For completeness its interesting to take a look at the execution plans the database engine generates. Here are Execution plans for input set of size 10 with person table containing 1000 records.

## ExistsStrategy ##
```
Nested Loop  (cost=3.20..91.52 rows=1 width=9) (actual time=0.074..0.074 rows=0 loops=1)
  Output: p.name
  Join Filter: (a.person_id = a_9.person_id)
  ->  Nested Loop  (cost=2.91..83.20 rows=1 width=49) (actual time=0.072..0.072 rows=0 loops=1)
        Output: p.name, p.id, a.person_id, a_1.person_id, a_2.person_id, a_3.person_id, a_4.person_id, a_5.person_id, a_6.person_id, a_7.person_id, a_8.person_id
        Join Filter: (a.person_id = a_8.person_id)
        ->  Nested Loop  (cost=2.61..74.87 rows=1 width=45) (actual time=0.071..0.071 rows=0 loops=1)
              Output: p.name, p.id, a.person_id, a_1.person_id, a_2.person_id, a_3.person_id, a_4.person_id, a_5.person_id, a_6.person_id, a_7.person_id
              Join Filter: (a.person_id = a_7.person_id)
              ->  Nested Loop  (cost=2.32..66.55 rows=1 width=41) (actual time=0.069..0.069 rows=0 loops=1)
                    Output: p.name, p.id, a.person_id, a_1.person_id, a_2.person_id, a_3.person_id, a_4.person_id, a_5.person_id, a_6.person_id
                    Join Filter: (a.person_id = a_6.person_id)
                    ->  Nested Loop  (cost=2.03..58.23 rows=1 width=37) (actual time=0.068..0.068 rows=0 loops=1)
                          Output: p.name, p.id, a.person_id, a_1.person_id, a_2.person_id, a_3.person_id, a_4.person_id, a_5.person_id
                          Join Filter: (a.person_id = a_5.person_id)
                          ->  Nested Loop  (cost=1.74..49.91 rows=1 width=33) (actual time=0.066..0.066 rows=0 loops=1)
                                Output: p.name, p.id, a.person_id, a_1.person_id, a_2.person_id, a_3.person_id, a_4.person_id
                                Join Filter: (a.person_id = a_4.person_id)
                                ->  Nested Loop  (cost=1.44..41.58 rows=1 width=29) (actual time=0.065..0.065 rows=0 loops=1)
                                      Output: p.name, p.id, a.person_id, a_1.person_id, a_2.person_id, a_3.person_id
                                      Join Filter: (a.person_id = a_3.person_id)
                                      ->  Nested Loop  (cost=1.15..33.26 rows=1 width=25) (actual time=0.063..0.063 rows=0 loops=1)
                                            Output: p.name, p.id, a.person_id, a_1.person_id, a_2.person_id
                                            Join Filter: (a.person_id = a_2.person_id)
                                            ->  Nested Loop  (cost=0.86..24.94 rows=1 width=21) (actual time=0.062..0.062 rows=0 loops=1)
                                                  Output: p.name, p.id, a.person_id, a_1.person_id
                                                  Join Filter: (a.person_id = a_1.person_id)
                                                  Rows Removed by Join Filter: 1
                                                  ->  Nested Loop  (cost=0.57..16.62 rows=1 width=17) (actual time=0.034..0.038 rows=1 loops=1)
                                                        Output: p.name, p.id, a.person_id
                                                        ->  Index Scan using address_pkey on public.address a  (cost=0.29..8.31 rows=1 width=4) (actual time=0.025..0.026 rows=1 loops=1)
                                                              Output: a.id, a.person_id, a.street
                                                              Index Cond: (a.id = 4205)
                                                        ->  Index Scan using person_pkey on public.person p  (cost=0.28..8.29 rows=1 width=13) (actual time=0.005..0.006 rows=1 loops=1)
                                                              Output: p.id, p.name
                                                              Index Cond: (p.id = a.person_id)
                                                  ->  Index Scan using address_pkey on public.address a_1  (cost=0.29..8.31 rows=1 width=4) (actual time=0.011..0.012 rows=1 loops=1)
                                                        Output: a_1.id, a_1.person_id, a_1.street
                                                        Index Cond: (a_1.id = 47898)
                                            ->  Index Scan using address_pkey on public.address a_2  (cost=0.29..8.31 rows=1 width=4) (never executed)
                                                  Output: a_2.id, a_2.person_id, a_2.street
                                                  Index Cond: (a_2.id = 42134)
                                      ->  Index Scan using address_pkey on public.address a_3  (cost=0.29..8.31 rows=1 width=4) (never executed)
                                            Output: a_3.id, a_3.person_id, a_3.street
                                            Index Cond: (a_3.id = 39175)
                                ->  Index Scan using address_pkey on public.address a_4  (cost=0.29..8.31 rows=1 width=4) (never executed)
                                      Output: a_4.id, a_4.person_id, a_4.street
                                      Index Cond: (a_4.id = 89848)
                          ->  Index Scan using address_pkey on public.address a_5  (cost=0.29..8.31 rows=1 width=4) (never executed)
                                Output: a_5.id, a_5.person_id, a_5.street
                                Index Cond: (a_5.id = 83765)
                    ->  Index Scan using address_pkey on public.address a_6  (cost=0.29..8.31 rows=1 width=4) (never executed)
                          Output: a_6.id, a_6.person_id, a_6.street
                          Index Cond: (a_6.id = 73569)
              ->  Index Scan using address_pkey on public.address a_7  (cost=0.29..8.31 rows=1 width=4) (never executed)
                    Output: a_7.id, a_7.person_id, a_7.street
                    Index Cond: (a_7.id = 35710)
        ->  Index Scan using address_pkey on public.address a_8  (cost=0.29..8.31 rows=1 width=4) (never executed)
              Output: a_8.id, a_8.person_id, a_8.street
              Index Cond: (a_8.id = 86086)
  ->  Index Scan using address_pkey on public.address a_9  (cost=0.29..8.31 rows=1 width=4) (never executed)
        Output: a_9.id, a_9.person_id, a_9.street
        Index Cond: (a_9.id = 76156)
Total runtime: 0.329 ms

```
## GroupByStrategy ##
```
HashAggregate  (cost=108.44..108.56 rows=10 width=9) (actual time=1.862..1.862 rows=0 loops=1)
  Output: p.name
  Filter: (count(*) >= 10)
  Rows Removed by Filter: 10
  ->  Hash Join  (cost=71.53..108.39 rows=10 width=9) (actual time=1.803..1.844 rows=10 loops=1)
        Output: p.name
        Hash Cond: (a.person_id = p.id)
        ->  Bitmap Heap Scan on public.address a  (cost=43.03..79.75 rows=10 width=4) (actual time=0.107..0.130 rows=10 loops=1)
              Output: a.id, a.person_id, a.street
              Recheck Cond: ((a.id = 4205) OR (a.id = 47898) OR (a.id = 42134) OR (a.id = 39175) OR (a.id = 89848) OR (a.id = 83765) OR (a.id = 73569) OR (a.id = 35710) OR (a.id = 86086) OR (a.id = 76156))
              ->  BitmapOr  (cost=43.03..43.03 rows=10 width=0) (actual time=0.099..0.099 rows=0 loops=1)
                    ->  Bitmap Index Scan on address_pkey  (cost=0.00..4.30 rows=1 width=0) (actual time=0.015..0.015 rows=1 loops=1)
                          Index Cond: (a.id = 4205)
                    ->  Bitmap Index Scan on address_pkey  (cost=0.00..4.30 rows=1 width=0) (actual time=0.013..0.013 rows=1 loops=1)
                          Index Cond: (a.id = 47898)
                    ->  Bitmap Index Scan on address_pkey  (cost=0.00..4.30 rows=1 width=0) (actual time=0.008..0.008 rows=1 loops=1)
                          Index Cond: (a.id = 42134)
                    ->  Bitmap Index Scan on address_pkey  (cost=0.00..4.30 rows=1 width=0) (actual time=0.007..0.007 rows=1 loops=1)
                          Index Cond: (a.id = 39175)
                    ->  Bitmap Index Scan on address_pkey  (cost=0.00..4.30 rows=1 width=0) (actual time=0.009..0.009 rows=1 loops=1)
                          Index Cond: (a.id = 89848)
                    ->  Bitmap Index Scan on address_pkey  (cost=0.00..4.30 rows=1 width=0) (actual time=0.003..0.003 rows=1 loops=1)
                          Index Cond: (a.id = 83765)
                    ->  Bitmap Index Scan on address_pkey  (cost=0.00..4.30 rows=1 width=0) (actual time=0.010..0.010 rows=1 loops=1)
                          Index Cond: (a.id = 73569)
                    ->  Bitmap Index Scan on address_pkey  (cost=0.00..4.30 rows=1 width=0) (actual time=0.009..0.009 rows=1 loops=1)
                          Index Cond: (a.id = 35710)
                    ->  Bitmap Index Scan on address_pkey  (cost=0.00..4.30 rows=1 width=0) (actual time=0.009..0.009 rows=1 loops=1)
                          Index Cond: (a.id = 86086)
                    ->  Bitmap Index Scan on address_pkey  (cost=0.00..4.30 rows=1 width=0) (actual time=0.008..0.008 rows=1 loops=1)
                          Index Cond: (a.id = 76156)
        ->  Hash  (cost=16.00..16.00 rows=1000 width=13) (actual time=1.678..1.678 rows=1000 loops=1)
              Output: p.name, p.id
              Buckets: 1024  Batches: 1  Memory Usage: 47kB
              ->  Seq Scan on public.person p  (cost=0.00..16.00 rows=1000 width=13) (actual time=0.007..0.813 rows=1000 loops=1)
                    Output: p.name, p.id
Total runtime: 2.034 ms

```
## IntersectStrategy ##
```
HashSetOp Intersect  (cost=0.57..166.29 rows=1 width=9) (actual time=0.333..0.333 rows=0 loops=1)
  Output: "*SELECT* 1".name, (0)
  ->  Append  (cost=0.57..166.29 rows=2 width=9) (actual time=0.323..0.330 rows=1 loops=1)
        ->  Result  (cost=0.57..149.66 rows=1 width=9) (actual time=0.306..0.306 rows=0 loops=1)
              Output: "*SELECT* 1".name, 0
              ->  HashSetOp Intersect  (cost=0.57..149.66 rows=1 width=9) (actual time=0.304..0.304 rows=0 loops=1)
                    Output: "*SELECT* 1".name, (0)
                    ->  Append  (cost=0.57..149.66 rows=2 width=9) (actual time=0.295..0.301 rows=1 loops=1)
                          ->  Result  (cost=0.57..133.03 rows=1 width=9) (actual time=0.277..0.277 rows=0 loops=1)
                                Output: "*SELECT* 1".name, 0
                                ->  HashSetOp Intersect  (cost=0.57..133.03 rows=1 width=9) (actual time=0.275..0.275 rows=0 loops=1)
                                      Output: "*SELECT* 1".name, (0)
                                      ->  Append  (cost=0.57..133.03 rows=2 width=9) (actual time=0.266..0.272 rows=1 loops=1)
                                            ->  Result  (cost=0.57..116.41 rows=1 width=9) (actual time=0.248..0.248 rows=0 loops=1)
                                                  Output: "*SELECT* 1".name, 0
                                                  ->  HashSetOp Intersect  (cost=0.57..116.41 rows=1 width=9) (actual time=0.247..0.247 rows=0 loops=1)
                                                        Output: "*SELECT* 1".name, (0)
                                                        ->  Append  (cost=0.57..116.40 rows=2 width=9) (actual time=0.237..0.244 rows=1 loops=1)
                                                              ->  Result  (cost=0.57..99.78 rows=1 width=9) (actual time=0.211..0.211 rows=0 loops=1)
                                                                    Output: "*SELECT* 1".name, 0
                                                                    ->  HashSetOp Intersect  (cost=0.57..99.78 rows=1 width=9) (actual time=0.209..0.209 rows=0 loops=1)
                                                                          Output: "*SELECT* 1".name, (0)
                                                                          ->  Append  (cost=0.57..99.77 rows=2 width=9) (actual time=0.201..0.208 rows=1 loops=1)
                                                                                ->  Result  (cost=0.57..83.15 rows=1 width=9) (actual time=0.187..0.187 rows=0 loops=1)
                                                                                      Output: "*SELECT* 1".name, 0
                                                                                      ->  HashSetOp Intersect  (cost=0.57..83.15 rows=1 width=9) (actual time=0.186..0.186 rows=0 loops=1)
                                                                                            Output: "*SELECT* 1".name, (0)
                                                                                            ->  Append  (cost=0.57..83.14 rows=2 width=9) (actual time=0.173..0.182 rows=1 loops=1)
                                                                                                  ->  Result  (cost=0.57..66.52 rows=1 width=9) (actual time=0.143..0.143 rows=0 loops=1)
                                                                                                        Output: "*SELECT* 1".name, 0
                                                                                                        ->  HashSetOp Intersect  (cost=0.57..66.52 rows=1 width=9) (actual time=0.142..0.142 rows=0 loops=1)
                                                                                                              Output: "*SELECT* 1".name, (0)
                                                                                                              ->  Append  (cost=0.57..66.51 rows=2 width=9) (actual time=0.131..0.138 rows=1 loops=1)
                                                                                                                    ->  Result  (cost=0.57..49.89 rows=1 width=9) (actual time=0.106..0.106 rows=0 loops=1)
                                                                                                                          Output: "*SELECT* 1".name, 0
                                                                                                                          ->  HashSetOp Intersect  (cost=0.57..49.89 rows=1 width=9) (actual time=0.104..0.104 rows=0 loops=1)
                                                                                                                                Output: "*SELECT* 1".name, (0)
                                                                                                                                ->  Append  (cost=0.57..49.88 rows=2 width=9) (actual time=0.095..0.102 rows=1 loops=1)
                                                                                                                                      ->  Result  (cost=0.57..33.26 rows=1 width=9) (actual time=0.077..0.077 rows=0 loops=1)
                                                                                                                                            Output: "*SELECT* 1".name, 0
                                                                                                                                            ->  HashSetOp Intersect  (cost=0.57..33.26 rows=1 width=9) (actual time=0.076..0.076 rows=0 loops=1)
                                                                                                                                                  Output: "*SELECT* 1".name, (0)
                                                                                                                                                  ->  Append  (cost=0.57..33.25 rows=2 width=9) (actual time=0.029..0.071 rows=2 loops=1)
                                                                                                                                                        ->  Subquery Scan on "*SELECT* 1"  (cost=0.57..16.63 rows=1 width=9) (actual time=0.027..0.033 rows=1 loops=1)
                                                                                                                                                              Output: "*SELECT* 1".name, 0
                                                                                                                                                              ->  Nested Loop  (cost=0.57..16.62 rows=1 width=9) (actual time=0.025..0.029 rows=1 loops=1)
                                                                                                                                                                    Output: p.name
                                                                                                                                                                    ->  Index Scan using address_pkey on public.address a  (cost=0.29..8.31 rows=1 width=4) (actual time=0.018..0.019 rows=1 loops=1)
                                                                                                                                                                          Output: a.id, a.person_id, a.street
                                                                                                                                                                          Index Cond: (a.id = 4205)
                                                                                                                                                                    ->  Index Scan using person_pkey on public.person p  (cost=0.28..8.29 rows=1 width=13) (actual time=0.003..0.004 rows=1 loops=1)
                                                                                                                                                                          Output: p.id, p.name
                                                                                                                                                                          Index Cond: (p.id = a.person_id)
                                                                                                                                                        ->  Subquery Scan on "*SELECT* 2"  (cost=0.57..16.63 rows=1 width=9) (actual time=0.027..0.033 rows=1 loops=1)
                                                                                                                                                              Output: "*SELECT* 2".name, 1
                                                                                                                                                              ->  Nested Loop  (cost=0.57..16.62 rows=1 width=9) (actual time=0.026..0.030 rows=1 loops=1)
                                                                                                                                                                    Output: p_1.name
                                                                                                                                                                    ->  Index Scan using address_pkey on public.address a_1  (cost=0.29..8.31 rows=1 width=4) (actual time=0.009..0.010 rows=1 loops=1)
                                                                                                                                                                          Output: a_1.id, a_1.person_id, a_1.street
                                                                                                                                                                          Index Cond: (a_1.id = 47898)
                                                                                                                                                                    ->  Index Scan using person_pkey on public.person p_1  (cost=0.28..8.29 rows=1 width=13) (actual time=0.014..0.015 rows=1 loops=1)
                                                                                                                                                                          Output: p_1.id, p_1.name
                                                                                                                                                                          Index Cond: (p_1.id = a_1.person_id)
                                                                                                                                      ->  Subquery Scan on "*SELECT* 3"  (cost=0.57..16.63 rows=1 width=9) (actual time=0.016..0.022 rows=1 loops=1)
                                                                                                                                            Output: "*SELECT* 3".name, 1
                                                                                                                                            ->  Nested Loop  (cost=0.57..16.62 rows=1 width=9) (actual time=0.014..0.018 rows=1 loops=1)
                                                                                                                                                  Output: p_2.name
                                                                                                                                                  ->  Index Scan using address_pkey on public.address a_2  (cost=0.29..8.31 rows=1 width=4) (actual time=0.008..0.009 rows=1 loops=1)
                                                                                                                                                        Output: a_2.id, a_2.person_id, a_2.street
                                                                                                                                                        Index Cond: (a_2.id = 42134)
                                                                                                                                                  ->  Index Scan using person_pkey on public.person p_2  (cost=0.28..8.29 rows=1 width=13) (actual time=0.003..0.004 rows=1 loops=1)
                                                                                                                                                        Output: p_2.id, p_2.name
                                                                                                                                                        Index Cond: (p_2.id = a_2.person_id)
                                                                                                                    ->  Subquery Scan on "*SELECT* 4"  (cost=0.57..16.63 rows=1 width=9) (actual time=0.022..0.028 rows=1 loops=1)
                                                                                                                          Output: "*SELECT* 4".name, 1
                                                                                                                          ->  Nested Loop  (cost=0.57..16.62 rows=1 width=9) (actual time=0.020..0.024 rows=1 loops=1)
                                                                                                                                Output: p_3.name
                                                                                                                                ->  Index Scan using address_pkey on public.address a_3  (cost=0.29..8.31 rows=1 width=4) (actual time=0.008..0.009 rows=1 loops=1)
                                                                                                                                      Output: a_3.id, a_3.person_id, a_3.street
                                                                                                                                      Index Cond: (a_3.id = 39175)
                                                                                                                                ->  Index Scan using person_pkey on public.person p_3  (cost=0.28..8.29 rows=1 width=13) (actual time=0.010..0.011 rows=1 loops=1)
                                                                                                                                      Output: p_3.id, p_3.name
                                                                                                                                      Index Cond: (p_3.id = a_3.person_id)
                                                                                                  ->  Subquery Scan on "*SELECT* 5"  (cost=0.57..16.63 rows=1 width=9) (actual time=0.027..0.033 rows=1 loops=1)
                                                                                                        Output: "*SELECT* 5".name, 1
                                                                                                        ->  Nested Loop  (cost=0.57..16.62 rows=1 width=9) (actual time=0.025..0.029 rows=1 loops=1)
                                                                                                              Output: p_4.name
                                                                                                              ->  Index Scan using address_pkey on public.address a_4  (cost=0.29..8.31 rows=1 width=4) (actual time=0.015..0.016 rows=1 loops=1)
                                                                                                                    Output: a_4.id, a_4.person_id, a_4.street
                                                                                                                    Index Cond: (a_4.id = 89848)
                                                                                                              ->  Index Scan using person_pkey on public.person p_4  (cost=0.28..8.29 rows=1 width=13) (actual time=0.008..0.009 rows=1 loops=1)
                                                                                                                    Output: p_4.id, p_4.name
                                                                                                                    Index Cond: (p_4.id = a_4.person_id)
                                                                                ->  Subquery Scan on "*SELECT* 6"  (cost=0.57..16.63 rows=1 width=9) (actual time=0.011..0.017 rows=1 loops=1)
                                                                                      Output: "*SELECT* 6".name, 1
                                                                                      ->  Nested Loop  (cost=0.57..16.62 rows=1 width=9) (actual time=0.010..0.014 rows=1 loops=1)
                                                                                            Output: p_5.name
                                                                                            ->  Index Scan using address_pkey on public.address a_5  (cost=0.29..8.31 rows=1 width=4) (actual time=0.004..0.005 rows=1 loops=1)
                                                                                                  Output: a_5.id, a_5.person_id, a_5.street
                                                                                                  Index Cond: (a_5.id = 83765)
                                                                                            ->  Index Scan using person_pkey on public.person p_5  (cost=0.28..8.29 rows=1 width=13) (actual time=0.003..0.004 rows=1 loops=1)
                                                                                                  Output: p_5.id, p_5.name
                                                                                                  Index Cond: (p_5.id = a_5.person_id)
                                                              ->  Subquery Scan on "*SELECT* 7"  (cost=0.57..16.63 rows=1 width=9) (actual time=0.023..0.029 rows=1 loops=1)
                                                                    Output: "*SELECT* 7".name, 1
                                                                    ->  Nested Loop  (cost=0.57..16.62 rows=1 width=9) (actual time=0.023..0.027 rows=1 loops=1)
                                                                          Output: p_6.name
                                                                          ->  Index Scan using address_pkey on public.address a_6  (cost=0.29..8.31 rows=1 width=4) (actual time=0.011..0.012 rows=1 loops=1)
                                                                                Output: a_6.id, a_6.person_id, a_6.street
                                                                                Index Cond: (a_6.id = 73569)
                                                                          ->  Index Scan using person_pkey on public.person p_6  (cost=0.28..8.29 rows=1 width=13) (actual time=0.009..0.010 rows=1 loops=1)
                                                                                Output: p_6.id, p_6.name
                                                                                Index Cond: (p_6.id = a_6.person_id)
                                            ->  Subquery Scan on "*SELECT* 8"  (cost=0.57..16.63 rows=1 width=9) (actual time=0.015..0.020 rows=1 loops=1)
                                                  Output: "*SELECT* 8".name, 1
                                                  ->  Nested Loop  (cost=0.57..16.62 rows=1 width=9) (actual time=0.014..0.018 rows=1 loops=1)
                                                        Output: p_7.name
                                                        ->  Index Scan using address_pkey on public.address a_7  (cost=0.29..8.31 rows=1 width=4) (actual time=0.010..0.011 rows=1 loops=1)
                                                              Output: a_7.id, a_7.person_id, a_7.street
                                                              Index Cond: (a_7.id = 35710)
                                                        ->  Index Scan using person_pkey on public.person p_7  (cost=0.28..8.29 rows=1 width=13) (actual time=0.002..0.003 rows=1 loops=1)
                                                              Output: p_7.id, p_7.name
                                                              Index Cond: (p_7.id = a_7.person_id)
                          ->  Subquery Scan on "*SELECT* 9"  (cost=0.57..16.63 rows=1 width=9) (actual time=0.016..0.021 rows=1 loops=1)
                                Output: "*SELECT* 9".name, 1
                                ->  Nested Loop  (cost=0.57..16.62 rows=1 width=9) (actual time=0.015..0.019 rows=1 loops=1)
                                      Output: p_8.name
                                      ->  Index Scan using address_pkey on public.address a_8  (cost=0.29..8.31 rows=1 width=4) (actual time=0.010..0.011 rows=1 loops=1)
                                            Output: a_8.id, a_8.person_id, a_8.street
                                            Index Cond: (a_8.id = 86086)
                                      ->  Index Scan using person_pkey on public.person p_8  (cost=0.28..8.29 rows=1 width=13) (actual time=0.002..0.003 rows=1 loops=1)
                                            Output: p_8.id, p_8.name
                                            Index Cond: (p_8.id = a_8.person_id)
        ->  Subquery Scan on "*SELECT* 10"  (cost=0.57..16.63 rows=1 width=9) (actual time=0.016..0.021 rows=1 loops=1)
              Output: "*SELECT* 10".name, 1
              ->  Nested Loop  (cost=0.57..16.62 rows=1 width=9) (actual time=0.015..0.019 rows=1 loops=1)
                    Output: p_9.name
                    ->  Index Scan using address_pkey on public.address a_9  (cost=0.29..8.31 rows=1 width=4) (actual time=0.009..0.010 rows=1 loops=1)
                          Output: a_9.id, a_9.person_id, a_9.street
                          Index Cond: (a_9.id = 76156)
                    ->  Index Scan using person_pkey on public.person p_9  (cost=0.28..8.29 rows=1 width=13) (actual time=0.003..0.004 rows=1 loops=1)
                          Output: p_9.id, p_9.name
                          Index Cond: (p_9.id = a_9.person_id)
Total runtime: 0.806 ms

```

