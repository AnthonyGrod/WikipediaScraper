# Wikipedia Scraper

### Overview

This project implements an algorithm which for two given articles finds all shortest paths between them.
The search engine performs a BFS using tail recursion, starting from article provided as an entry point
in the input file.

### Algorithm in detail

In each recursion call a queue of list of links (the list of links is representing their path from starting article) to process
and a list of results (paths found) are passed. For a given article link taken from the head of a dequeued path the engine checks:

1. If results is not empty and length of the path we're currently processing is greater than length of the path in results.
   If both conditions are met, we stop the search since all shortest paths have been found.
2. If it has been visited. If so, we just call the recursive function passing the queue without the link.
3. If it is the endpoint we're looking for. If so, we add it to the results list and go on.
4. In other case we read all Wikipedia links inside given article and enqueue them with their path from the starting article
   and continue the search.

The search begins with only starting article on the queue.

### Requirements

In order for this project to work you need to have:

* Scala version minimum 2.13.8
* sbt version minimum 1.8.3 (https://www.scala-sbt.org/download.html)

### Launch

1. In the directory with ``build.sbt`` file run ``sbt`` command in your terminal.
2. Once ``sbt`` is up and running, inside ``sbt`` run ``compile``. This will automatically create necessary dependencies, compile files and link them.
3. Now you just have to run the project. For details, please look below.

### Run

Once you have compiled the project and downloaded necessary requirements, you can run
the app inside sbt with command

``run <absoluteInputPath> <absoluteOutputPath>``

``absoluteInputPath`` is an absolute path to your input txt file
with sites between which you'd like to know shortest paths. For detailed information
regarding the type of input, please look below.

``absoluteOutputPath`` is an absolute path to the txt file where you want to store
the output. The path must be valid but the txt file itself does not have to exist.
In that case, the program creates specified ``.txt`` file and places the output
right there. If file already exists, the program just overrides file's content.

### Input

The program requires a ``.txt`` file as its input. Each line of this file must be of type

``(langCode, srcName, destName)``

and ended by a newline character.

- ``langCode`` - valid language Wikipedia code (https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes, 639-1 column).
  For example for english it would be ``en`` or for polish ``pl``.
- ``srcName`` - valid Wikipedia article title from which you would like to start the search.. You can look it up in the url of your chosen
  Wikipedia article. For example for an article available at https://pl.wikipedia.org/wiki/Skala_betów the ``srcName``
  would be ``Skala_betów``.
- ``destName`` - valid Wikipedia article title to which you would like to get all shortest paths from ``srcName`` article.

In case of an incorrect input an appropriate message will be shown.

### Output

As an output the program created or overrides ``.txt`` file at location provided by
``absoluteOutputPath`` argument in ``run`` command. In this file the program will
put all shortest paths for each line of input sorted alphabetically into output file.

### Testing

Automatic tests can be run in ``sbt`` using ``test`` command
