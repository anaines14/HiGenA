
def getLoadQuery(file, columns):
    load = f"""
            //Load gAeD3MTGCCv8YNTaK
            LOAD CSV WITH HEADERS FROM 'file:///{file}.csv' AS row"""

    for col in columns:


            WITH toInteger(row.cmd_i) AS cmd_i, toInteger(row.sat) AS sat, 
                row._id AS id, row.code AS code, row.derivationOf AS derivationOf,
                row.undirected AS undirected, row.weaklyConnected AS  weaklyConnected,
                row.stonglyConnected AS  stonglyConnected, row.transitive AS  transitive, 
                row.acyclic AS  acyclic, row.complete AS  complete,
                row.oriented AS  oriented, row.noLoops AS  noLoops
            MERGE (s:Submission {
                id: id,
                cmd_i: cmd_i,
                sat: sat,
                derivationOf: derivationOf,
                code: code,
                undirected: undirected,
                weaklyConnected: weaklyConnected,
                stronglyConnected: stonglyConnected,
                transitive: transitive,
                acyclic: acyclic,
                complete: complete,
                oriented: oriented,
                noLoops: noLoops
            })"""