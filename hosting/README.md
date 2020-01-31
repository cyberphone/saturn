### Saturn Payment Authorization System
This project holds the code for a Hosting service

It requires configuration in **merchant** and **payeebank** through the `-Dhostingoption=true` flag.

```code
ant saturn/bank tomcat -Dpayeebank=true -Dhostingoption=true
ant saturn/merchant tomcat -Dhostingoption=true
```
