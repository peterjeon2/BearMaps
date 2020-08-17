# BearMaps

This is the third project in CS61B - Data Structures, taught by Josh Hug at U.C. Berkeley.

# Project Specifications:
https://sp18.datastructur.es/materials/proj/proj3/proj3

This project is a web mapping application inspired by Google Maps. It covers the city of Berkeley and uses real world mapping data from the OpenStreetMap
project. I wrote the back-end for the application. The code I wrote can be found in the "/src/main" folder. 

My responsiblities included:
1) Map Rastering - converting the http request from the user and determining which images to be displayed on the web serve. 
2) Routing & Location Data - Parsing through routing and location data from an OSM XML file and storing data points in graphs.
3) Route Search - Implementing the A* Search Algorithm to find the shortest path between two locations.

My biggest challenges in this project were familiarizing myself with the XML markup language, understanding how to store the data into 
a graph class, and implementing a simple but efficient graph traversal. I overcame these issues by reading through a lot of documentation and
making use of the HashMap and HashSet data structures. The average runtime for my shortest-path method ended up being around ~2 times 
faster than the staff solution.

# Deployment Instructions 
In order to view the project, please refer to the build instructions in the project spec. The project uses apache maven as its build system and integrates with IntelliJ. Run MapServer.java and type in localhost:4567 into your browser.

Heroku Deployment hopefully coming soon.
