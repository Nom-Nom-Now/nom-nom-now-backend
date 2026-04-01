# Paginable endpint get recipes

the endpoint getAllRecipes is paginable, which means that you can specify the page and the number of items per page in the query parameters.
For example, if you want to get the first page with 10 items per page, you can use the following query parameters:

``` 
/recipes?page=1&per_page=10
```

page 1 is ?page=0
page 2 is ?page=1
...
