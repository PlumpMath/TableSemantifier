# TableSemantifier
Annotates a table with type of each column, binary relation between columns and disambiguation of cell text to Knowledgebase.  
The software adds meaning to your tables.

##Introduction 
The implementation is loosly based on [Annotating and Searching Web Tables Using Entities, Types and Relationships](http://vldb.org/pvldb/vldb2010/papers/R118.pdf), a 2010 VLDB publication. 
I did not perform any training for parameter learning as explained in the publication.
I like it more when a data-agnostic model performs equally well. 
Besides, I am not totally convinced why training is required except for upping the values for a publication.  
Also, unlike the system described in the paper that uses YAGO, this uses Wikidata instead.
Because, Freebase recently migrated to Wikidata (need I say more?)

##Usage
The system works by making several requests to Wikidata in parallel and can have choking effect on the endpoint. Please use it sparingly. 
If you want to semantify large number of tables, then consider setting up your version of the Server.
It is nice of Wikidata to not put query limits, so let's behave!  
Start with `TableSemantifier.main` for some test examples. In the meanwhile, I will work towards a better API. 

##Examples
Some example tables and their semantified versions are shown below.
A table is added with five most likely annotations for each of: cell text, headers and binary relations.

###Authors and their notable work.
<table>
<tr>
<td>
<table style='display:inline-block'>
<tbody>
<tr><td>Rowling</td><td>Harry Potter</td></tr>
<tr><td>Tolkien</td><td>Lord of the Rings</td></tr>
<tr><td>Martin</td><td>Game of Thrones</td></tr>
<tr><td>Dickens</td><td>Great Expectations</td></tr>
<tr><td>Mark</td><td>Huckleberry Finn</td></tr>
<tr><td>Leo</td><td>War and Peace</td></tr>
<tr><td>Conan Doyle</td><td>Sherlock Holmes</td></tr>
<tr><td>William</td><td>Hamlet</td></tr>
<tr><td>Dante</td><td>Inferno</td></tr>
</tbody>
</table>
</td>
<td>
<table>
<thead><th><br><a href='http://www.wikidata.org/prop/direct/P1412:::http://www.wikidata.org/entity/Q1860'>"languages spoken, written or signed"@en:::"English"@en</a><br><a href='http://www.wikidata.org/prop/direct/P106:::http://www.wikidata.org/entity/Q36180'>"occupation"@en:::"writer"@en</a><br><a href='http://www.wikidata.org/prop/direct/P106:::http://www.wikidata.org/entity/Q28389'>"occupation"@en:::"screenwriter"@en</a><br><a href='http://www.wikidata.org/prop/direct/P31:::http://www.wikidata.org/entity/Q12308941'>"instance of"@en:::"male given name"@en</a><br><a href='http://www.wikidata.org/prop/direct/P31:::http://www.wikidata.org/entity/Q5'>"instance of"@en:::"human"@en</a></th><th><br><a href='http://www.wikidata.org/prop/direct/P364:::http://www.wikidata.org/entity/Q1860'>"original language of work"@en:::"English"@en</a><br><a href='http://www.wikidata.org/prop/direct/P31:::http://www.wikidata.org/entity/Q571'>"instance of"@en:::"book"@en</a><br><a href='http://www.wikidata.org/prop/direct/P495:::http://www.wikidata.org/entity/Q30'>"country of origin"@en:::"United States of America"@en</a><br><a href='http://www.wikidata.org/prop/direct/P136:::http://www.wikidata.org/entity/Q1257444'>"genre"@en:::"film adaptation"@en</a><br><a href='http://www.wikidata.org/prop/direct/P31:::http://www.wikidata.org/entity/Q11424'>"instance of"@en:::"film"@en</a></th></thead>
<tbody>
<tr><td>JK Rowling<br><a href='http://www.wikidata.org/entity/Q34660'>"Joanne K. Rowling"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q1343175'>"Magic Beyond Words"@en</a><br><a href='http://www.wikidata.org/entity/Q2838011'>"Scorpius Malfoy"@en</a><br><a href='http://www.wikidata.org/entity/Q26081141'>null</a></td><td>Harry Potter<br><a href='http://www.wikidata.org/entity/Q8337'>"Harry Potter"@en</a><br><a href='http://www.wikidata.org/entity/Q43361'>"Harry Potter and the Philosopher's Stone"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q5410773'>"Harry Potter universe"@en</a><br><a href='http://www.wikidata.org/entity/Q388591'>"Harry Potter fandom"@en</a></td></tr>
<tr><td>Tolkien<br><a href='http://www.wikidata.org/entity/Q892'>"J. R. R. Tolkien"@en</a><br><a href='http://www.wikidata.org/entity/Q82032'>"Christopher Tolkien"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q81738'>"Tolkien's legendarium"@en</a><br><a href='http://www.wikidata.org/entity/Q82087'>"Tim Tolkien"@en</a></td><td>Lord of the Rings<br><a href='http://www.wikidata.org/entity/Q15228'>"The Lord of the Rings"@en</a><br><a href='http://www.wikidata.org/entity/Q127367'>"The Lord of the Rings: The Fellowship of the Ring"@en</a><br><a href='http://www.wikidata.org/entity/Q164963'>"The Lord of the Rings: The Two Towers"@en</a><br><a href='http://www.wikidata.org/entity/Q131074'>"The Lord of the Rings: The Return of the King"@en</a><br><a href='http://www.wikidata.org/entity/Q502902'>"The History of The Lord of the Rings"@en</a></td></tr>
<tr><td>Martin<br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q18002399'>"Martin"@en</a><br><a href='http://www.wikidata.org/entity/Q18002404'>"Martín"@en</a><br><a href='http://www.wikidata.org/entity/Q17744604'>"Martin"@en</a><br><a href='http://www.wikidata.org/entity/Q27001'>"Martin"@en</a></td><td>Game of Thrones<br><a href='http://www.wikidata.org/entity/Q23572'>"Game of Thrones"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q20086263'>"Game of Thrones character"@en</a><br><a href='http://www.wikidata.org/entity/Q1658029'>"Game of Thrones (season 1)"@en</a><br><a href='http://www.wikidata.org/entity/Q302358'>"Game of Thrones (season 2)"@en</a></td></tr>
<tr><td>Dickens<br><a href='http://www.wikidata.org/entity/Q5686'>"Charles Dickens"@en</a><br><a href='http://www.wikidata.org/entity/Q269894'>"Kim Dickens"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q3850458'>"Mary Dickens"@en</a><br><a href='http://www.wikidata.org/entity/Q2625262'>"Catherine Dickens"@en</a></td><td>Great Expectations<br><a href='http://www.wikidata.org/entity/Q219552'>"Great Expectations"@en</a><br><a href='http://www.wikidata.org/entity/Q591036'>"Great Expectations"@en</a><br><a href='http://www.wikidata.org/entity/Q24801784'>"Genome sequences and great expectations"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q23793544'>"Great Expectations"@en</a></td></tr>
<tr><td>Mark<br><a href='http://www.wikidata.org/entity/Q7245'>"Mark Twain"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q13610143'>"Mark"@en</a><br><a href='http://www.wikidata.org/entity/Q16279012'>"Márk"@en</a><br><a href='http://www.wikidata.org/entity/Q500153'>"Mark Municipality"@en</a></td><td>Huckleberry Finn<br><a href='http://www.wikidata.org/entity/Q215410'>"Adventures of Huckleberry Finn"@en</a><br><a href='http://www.wikidata.org/entity/Q965149'>"Huckleberry Finn"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q48479'>"Huckleberry Finn"@en</a><br><a href='http://www.wikidata.org/entity/Q4957947'>"Jim"@en</a></td></tr>
<tr><td>Leo<br><a href='http://www.wikidata.org/entity/Q6524042'>"Leo McGarry"@en</a><br><a href='http://www.wikidata.org/entity/Q332530'>"Leo McCarey"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q606764'>"Leo"@en</a><br><a href='http://www.wikidata.org/entity/Q19688687'>"Léo"@en</a></td><td>War and Peace<br><a href='http://www.wikidata.org/entity/Q643811'>"War and Peace"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q845176'>"War and Peace"@en</a><br><a href='http://www.wikidata.org/entity/Q3566232'>"War and Peace: 1796–1815"@en</a><br><a href='http://www.wikidata.org/entity/Q161531'>"War and Peace"@en</a></td></tr>
<tr><td>Conan Doyle<br><a href='http://www.wikidata.org/entity/Q35610'>"Arthur Conan Doyle"@en</a><br><a href='http://www.wikidata.org/entity/Q1291544'>"Adrian Conan Doyle"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q1566651'>"Jean Conan Doyle"@en</a><br><a href='http://www.wikidata.org/entity/Q22704344'>"Denis Conan Doyle"@en</a></td><td>Sherlock Holmes<br><a href='http://www.wikidata.org/entity/Q2316684'>"canon of Sherlock Holmes"@en</a><br><a href='http://www.wikidata.org/entity/Q1638927'>"Sherlock Holmes Baffled"@en</a><br><a href='http://www.wikidata.org/entity/Q200396'>"Sherlock Holmes"@en</a><br><a href='http://www.wikidata.org/entity/Q392147'>"The Adventures of Sherlock Holmes"@en</a><br><a href='NA'>null</a></td></tr>
<tr><td>William<br><a href='http://www.wikidata.org/entity/Q692'>"William Shakespeare"@en</a><br><a href='http://www.wikidata.org/entity/Q1883000'>"William Johnson"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q12344159'>"William"@en</a><br><a href='http://www.wikidata.org/entity/Q15947614'>"William Arthur Jobson Archbold"@en</a></td><td>Hamlet<br><a href='http://www.wikidata.org/entity/Q41567'>"Hamlet"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q5084'>"hamlet"@en</a><br><a href='http://www.wikidata.org/entity/Q14422206'>"Hamlet, Prince of Denmark"@en</a><br><a href='http://www.wikidata.org/entity/Q1573602'>"Hamlet"@en</a></td></tr>
<tr><td>Dante<br><a href='http://www.wikidata.org/entity/Q186748'>"Dante Gabriel Rossetti"@en</a><br><a href='http://www.wikidata.org/entity/Q455279'>"Joe Dante"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q18182706'>"Dante"@en</a><br><a href='http://www.wikidata.org/entity/Q1067'>"Dante Alighieri"@en</a></td><td>Inferno<br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q908615'>"Inferno"@en</a><br><a href='http://www.wikidata.org/entity/Q4509219'>"Inferno"@en</a><br><a href='http://www.wikidata.org/entity/Q7186906'>"Phoenix Inferno"@en</a><br><a href='http://www.wikidata.org/entity/Q225340'>"Inferno"@en</a></td></tr>
</tbody>
</table>
</td>
</tr>
Possible relations between column 0 and 1 shown in the books table is <a href='[http://www.wikidata.org/prop/direct/P800, http://www.wikidata.org/prop/direct/P1445, http://www.wikidata.org/prop/direct/P31, http://www.wikidata.org/prop/direct/P1080, http://www.wikidata.org/prop/direct/P1441]'>["notable work"@en, "fictional universe described in"@en, "instance of"@en, "from fictional universe"@en, "present in work"@en]</a>

###Indian Cricketers
<table>
<tr>
<td>
<table>
<thead><th></th></thead>
<tbody>
<tr><td>Sachin</td></tr>
<tr><td>Sehwag</td></tr>
<tr><td>Mr. Dependable</td></tr>
<tr><td>Yuvi</td></tr>
<tr><td>Dhoni</td></tr>
<tr><td>lakshmipathi</td></tr>
</tbody>
</table>
</td>
<td>
<table>
<thead><th><br><a href='http://www.wikidata.org/prop/direct/P106:::http://www.wikidata.org/entity/Q12299841'>"occupation"@en:::"cricketer"@en</a><br><a href='http://www.wikidata.org/prop/direct/P641:::http://www.wikidata.org/entity/Q5375'>"sport"@en:::"cricket"@en</a><br><a href='http://www.wikidata.org/prop/direct/P166:::http://www.wikidata.org/entity/Q671622'>"award received"@en:::"Arjuna Award"@en</a><br><a href='http://www.wikidata.org/prop/direct/P166:::http://www.wikidata.org/entity/Q949193'>"award received"@en:::"Padma Shri"@en</a><br><a href='http://www.wikidata.org/prop/direct/P27:::http://www.wikidata.org/entity/Q668'>"country of citizenship"@en:::"India"@en</a></th></thead>
<tbody>
<tr><td>Sachin<br><a href='http://www.wikidata.org/entity/Q9488'>"Sachin Tendulkar"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q23041806'>"Sachin"@en</a><br><a href='http://www.wikidata.org/entity/Q544240'>"Sachin Bhowmick"@en</a><br><a href='http://www.wikidata.org/entity/Q3631431'>"Sachin"@en</a></td></tr>
<tr><td>Sehwag<br><a href='http://www.wikidata.org/entity/Q3345479'>"Virender Sehwag"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q20337202'>"Template:User Virender Sehwag"@en</a><br><a href='http://www.wikidata.org/entity/Q17656036'>"Cricket: Sehwag dominates"@en</a><br><a href='http://www.wikidata.org/entity/Q24699461'>null</a></td></tr>
<tr><td>Mr. Dependable<br><a href='http://www.wikidata.org/entity/Q470772'>"Rahul Dravid"@en</a><br><a href='NA'>null</a></td></tr>
<tr><td>Yuvi<br><a href='http://www.wikidata.org/entity/Q2723790'>"Yuvraj Singh"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q20294863'>null</a><br><a href='http://www.wikidata.org/entity/Q20210505'>null</a></td></tr>
<tr><td>Dhoni<br><a href='http://www.wikidata.org/entity/Q470774'>"Mahendra Singh Dhoni"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q18127580'>"M.S.Dhoni"@en</a><br><a href='http://www.wikidata.org/entity/Q253202'>"Dhoni"@en</a><br><a href='http://www.wikidata.org/entity/Q22351215'>"Sakshi Dhoni"@en</a></td></tr>
<tr><td>lakshmipathi<br><a href='http://www.wikidata.org/entity/Q3521491'>"Lakshmipathy Balaji"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q3453122'>"Rukmini Lakshmipathi"@en</a><br><a href='http://www.wikidata.org/entity/Q20116458'>"Lakshmipati"@en</a></td></tr>
</tbody>
</table>
</td>
</tr>
</table>

###PadmaVibhushan winners in 2016 (except Krishnamurthy)
<table>
<tr>
<td>
<table>
<thead><th></th></thead>
<tbody>
<tr><td>Krishnamurthy</td></tr>
<tr><td>Avinash Dixit</td></tr>
<tr><td>Rajinikanth</td></tr>
<tr><td>Ramoji</td></tr>
<tr><td>Ravi Shankar</td></tr>
<tr><td>Ambani</td></tr>
</tbody>
</table>
</td>
<td>
<table>
<thead><th><br><a href='http://www.wikidata.org/prop/direct/P166:::http://www.wikidata.org/entity/Q672392'>"award received"@en:::"Padma Vibhushan"@en</a><br><a href='http://www.wikidata.org/prop/direct/P106:::http://www.wikidata.org/entity/Q33999'>"occupation"@en:::"actor"@en</a><br><a href='http://www.wikidata.org/prop/direct/P27:::http://www.wikidata.org/entity/Q668'>"country of citizenship"@en:::"India"@en</a><br><a href='http://www.wikidata.org/prop/direct/P106:::http://www.wikidata.org/entity/Q3282637'>"occupation"@en:::"film producer"@en</a><br><a href='http://www.wikidata.org/prop/direct/P21:::http://www.wikidata.org/entity/Q6581097'>"sex or gender"@en:::"male"@en</a></th></thead>
<tbody>
<tr><td>Krishnamurthy<br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q1380058'>"Kalki Krishnamurthy"@en</a><br><a href='http://www.wikidata.org/entity/Q18350384'>"Hunsur Krishnamurthy"@en</a><br><a href='http://www.wikidata.org/entity/Q467315'>"Kavita Krishnamurthy"@en</a><br><a href='http://www.wikidata.org/entity/Q6323322'>"K. E. Krishnamurthy"@en</a></td></tr>
<tr><td>Avinash Dixit<br><a href='http://www.wikidata.org/entity/Q791030'>"Avinash Dixit"@en</a><br><a href='NA'>null</a></td></tr>
<tr><td>Rajinikanth<br><a href='http://www.wikidata.org/entity/Q60068'>"Rajinikanth"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q3522148'>"Soundarya Rajinikanth"@en</a><br><a href='http://www.wikidata.org/entity/Q6495834'>"Latha Rajinikanth"@en</a><br><a href='http://www.wikidata.org/entity/Q19895674'>"Rajinikanth: The Definitive Biography"@en</a></td></tr>
<tr><td>Ramoji<br><a href='http://www.wikidata.org/entity/Q7289797'>"Ramoji Rao"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q1786944'>"Ramoji Film City"@en</a><br><a href='http://www.wikidata.org/entity/Q16251693'>"Ramoji Group"@en</a></td></tr>
<tr><td>Ravi Shankar<br><a href='http://www.wikidata.org/entity/Q103774'>"Ravi Shankar"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q6323900'>"K. Ravi Shankar"@en</a><br><a href='http://www.wikidata.org/entity/Q7117492'>"P. Ravi Shankar"@en</a><br><a href='http://www.wikidata.org/entity/Q2770079'>"Ravi Shankar Vyas"@en</a></td></tr>
<tr><td>Ambani<br><a href='http://www.wikidata.org/entity/Q468364'>"Dhirubhai Ambani"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q2721694'>"Tina Ambani"@en</a><br><a href='http://www.wikidata.org/entity/Q298547'>"Mukesh Ambani"@en</a><br><a href='http://www.wikidata.org/entity/Q7041178'>"Nita Ambani"@en</a></td></tr>
</tbody>
</table>
</td>
</tr>
</table>

###Nobel prize winning physicists 
<table>
<tr>
<td>
<table>
<thead><th></th></thead>
<tbody>
<tr><td>Feynman</td></tr>
<tr><td>Einstein</td></tr>
<tr><td>Peter</td></tr>
<tr><td>Raman</td></tr>
<tr><td>Boyle</td></tr>
</tbody>
</table>
</td>
<td>
<table>
<thead><th><br><a href='http://www.wikidata.org/prop/direct/P1412:::http://www.wikidata.org/entity/Q1860'>"languages spoken, written or signed"@en:::"English"@en</a><br><a href='http://www.wikidata.org/prop/direct/P106:::http://www.wikidata.org/entity/Q169470'>"occupation"@en:::"physicist"@en</a><br><a href='http://www.wikidata.org/prop/direct/P21:::http://www.wikidata.org/entity/Q6581097'>"sex or gender"@en:::"male"@en</a><br><a href='http://www.wikidata.org/prop/direct/P31:::http://www.wikidata.org/entity/Q5'>"instance of"@en:::"human"@en</a><br><a href='http://www.wikidata.org/prop/direct/P106:::http://www.wikidata.org/entity/Q33999'>"occupation"@en:::"actor"@en</a></th></thead>
<tbody>
<tr><td>Feynman<br><a href='http://www.wikidata.org/entity/Q39246'>"Richard Feynman"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q985959'>"7495 Feynman"@en</a><br><a href='http://www.wikidata.org/entity/Q2373057'>"Joan Feynman"@en</a><br><a href='http://www.wikidata.org/entity/Q2743592'>"Surely You're Joking, Mr. Feynman!"@en</a></td></tr>
<tr><td>Einstein<br><a href='http://www.wikidata.org/entity/Q937'>"Albert Einstein"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q118253'>"Eduard Einstein"@en</a><br><a href='http://www.wikidata.org/entity/Q123371'>"Hans Albert Einstein"@en</a><br><a href='http://www.wikidata.org/entity/Q66282'>"Carl Einstein"@en</a></td></tr>
<tr><td>Peter<br><a href='http://www.wikidata.org/entity/Q610293'>"Peter Goldblatt"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q2793400'>"Peter"@en</a><br><a href='http://www.wikidata.org/entity/Q19018108'>"Péter"@en</a><br><a href='http://www.wikidata.org/entity/Q18156274'>"Hans-Peter"@en</a></td></tr>
<tr><td>Raman<br><a href='http://www.wikidata.org/entity/Q60429'>"C. V. Raman"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q19413922'>"Raman"@en</a><br><a href='http://www.wikidata.org/entity/Q3532965'>"Vimala Raman"@en</a><br><a href='http://www.wikidata.org/entity/Q476544'>"Raman"@en</a></td></tr>
<tr><td>Boyle<br><a href='http://www.wikidata.org/entity/Q134867'>"Danny Boyle"@en</a><br><a href='NA'>null</a><br><a href='http://www.wikidata.org/entity/Q315123'>"Peter Boyle"@en</a><br><a href='http://www.wikidata.org/entity/Q486103'>"Lara Flynn Boyle"@en</a><br><a href='http://www.wikidata.org/entity/Q20155801'>"Boyle"@en</a></td></tr>
</tbody>
</table>
</td>
</tr>
</table>

##Time complexity
The bottleneck is in accessing the network to fetch related content. 
The requests to the network are all parallelized and cached in this implementation. 
The typical running time varies depending on your machine and network. 
On my (10Mbps) network and on my 2.5Ghz Intelx64 Core i5 machine, the books table took around 2 minutes and the rest took less than a minute.  

##Need to fix
1. The caching implemented in Wikidata has issues. It sometimes throws java.io.OptionalDataException and hence not read completely because of which some requests are not cached at all.  
2. The suggestions retrieved in the initial stage of pipeline based on a cell text are crucial. Suggestions for entities work fine but fails for nouns such as *red*, *alpha*(greek alphabet) etc. One possible fix is to leverage the column text if available to direct the search query.  
3. Several corner cases are not handled such as when processing requests to knowledge base or when printing a table etc.
4. Needs a lot more documentation and proper interface for easier access.
5. The system fetches suggestions for text spanning the entire cell, it is straight forward to extend it to select the right chunk in cell text. For example, cell text *Stacy loves red color* should be treated as two cells since there are two possible chunks, *Stacy* and *red*.
