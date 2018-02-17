# git-prov

Git-Prov can be found at [git-prov]

**API Specification**

* **_`GET` /repos/organizations/{organization}_**

    <p>Gets a list of redirects to all repositories owned by an organization.</p>
    Example for GET /repos/organizations/Apache

```javascript
[
 "https://git-prov.herokuapp.com/repos/owner/Apache/tapestry3",
 "https://git-prov.herokuapp.com/repos/owner/Apache/apr-iconv",
 "https://git-prov.herokuapp.com/repos/owner/Apache/tapestry4",
 "https://git-prov.herokuapp.com/repos/owner/Apache/sling-old-svn-mirror",
 "https://git-prov.herokuapp.com/repos/owner/Apache/xalan-j",
 "https://git-prov.herokuapp.com/repos/owner/Apache/etch",
 "https://git-prov.herokuapp.com/repos/owner/Apache/apr",
 "https://git-prov.herokuapp.com/repos/owner/Apache/stdcxx",
 "https://git-prov.herokuapp.com/repos/owner/Apache/zookeeper",
 "https://git-prov.herokuapp.com/repos/owner/Apache/lucenenet",
                            ...
]
```

* **_`GET` /repos/owner/{owner}/{name}_**

    <p>Gets a provenance document corresponding with the repository denoted by name and owner.</p>
    Example for GET /repos/owner/stefan-matcovici/HuWr

```javascript
@prefix prov: <http://www.w3.org/ns/prov#>
   .
  @prefix xsd: <http://www.w3.org/2001/XMLSchema#>
     .
    @prefix foaf: <http://xmlns.com/foaf/0.1/>
     .
    @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
       .
      @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         .
        @prefix gitprov: <https://git-prov.herokuapp.com/repos/owner/stefan-matcovici/HuWr#>
           .


          gitprov:commit-1ba1353ce13ef6c63e2ecaaab657981357ab7804 a prov:Activity ;
            rdfs:label "Initial commit" .
                         ...

```

* **_`GET` /repos/search?{params}_**

    <p>Gets a list of redirects to all public repositories that match the query parameters.</p>

    **for more information about available values for query parameters see [github-search]

    Example for GET /repos/search?language=ruby


```javascript
[
  "http://localhost:8080/repos/owner/rails/rails",
  "http://localhost:8080/repos/owner/jekyll/jekyll",
  "http://localhost:8080/repos/owner/Homebrew/legacy-homebrew",
  "http://localhost:8080/repos/owner/discourse/discourse",
  "http://localhost:8080/repos/owner/bayandin/awesome-awesomeness",
  "http://localhost:8080/repos/owner/fastlane/fastlane",
  "http://localhost:8080/repos/owner/gitlabhq/gitlabhq",
  "http://localhost:8080/repos/owner/plataformatec/devise",
  "http://localhost:8080/repos/owner/huginn/huginn",
  "http://localhost:8080/repos/owner/jondot/awesome-react-native",
  "http://localhost:8080/repos/owner/Thibaut/devdocs",
  "http://localhost:8080/repos/owner/hashicorp/vagrant",
  "http://localhost:8080/repos/owner/ruby/ruby"
                            ...
]
```

* **_`GET` /repos/users/{user}_**

    <p>Gets a list of redirects to all public repositories of provided user</p>
    Example for GET /repos/users/stefan-matcovici

```javascript
[
  "https://git-prov-pr-3.herokuapp.com/repos/owner/stefan-matcovici/AI-Hanoi-Towers",
  "https://git-prov-pr-3.herokuapp.com/repos/owner/stefan-matcovici/Data-Structures",
  "https://git-prov-pr-3.herokuapp.com/repos/owner/stefan-matcovici/environmental-events",
  "https://git-prov-pr-3.herokuapp.com/repos/owner/stefan-matcovici/HuWr",
  "https://git-prov-pr-3.herokuapp.com/repos/owner/stefan-matcovici/Information-Security--Homework1",
  "https://git-prov-pr-3.herokuapp.com/repos/owner/stefan-matcovici/my-first-blog",
  "https://git-prov-pr-3.herokuapp.com/repos/owner/stefan-matcovici/random-words",
  "https://git-prov-pr-3.herokuapp.com/repos/owner/stefan-matcovici/VirtualSoc",
  "https://git-prov-pr-3.herokuapp.com/repos/owner/stefan-matcovici/VirtualSoc--ComputerNetworks"
                            ...
]
```


[git-prov]: https://git-prov.herokuapp.com/
[github-search]: https://developer.github.com/v3/search/
