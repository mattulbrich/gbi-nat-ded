# gbi-nat-ded
Webapp for the Natural Deduction Calculus as taught in the course [Grundbegriffe der
Informatik](https://formal.kastel.kit.edu/teaching/GBI23/) at
the [Informatics department at KIT](https://informatik.kit.edu).

In contrast to a number of different introductions to natural deduction, this
introduces the calculus using an analytic approach where a proof is cosntructed
starting at the formula which is to be proved.

A running instance of the app can be found at https://formal.kastel.kit.edu/~ulbrich/natuerlich/.
There is some extra documentation available there.

<img src="doubleNeg.png">

# Technicalities

The calculus runs as a Javascript application within your web browser.
The Javascript code is generated from Kotlin code that implements the
reasoning engine.

The formula which is to be proved, must be provided as an argument to
the website (as a query following a question mark).

## Simple compilation and installation

In order to obtain a working instance of the webapp, execute:
```
./gradlew jsBrowserDistribution
```
then copy the contents of the following directory to your (static) webserver
```
./build/dist/js/productionExecutable
```
The `index.html` can be browsed to run the app. Execute
```
./gradlew jsRun
```
to run the app in a local browser.