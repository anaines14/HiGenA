import pandas as pd
import os


def getLoadQuery(challenge, pred, dict):
    df = dict[challenge][pred]
    columns = df.dtypes
    filename = challenge + "/" + pred + ".csv"

    load = "//Load " + challenge + "/" + pred
    load += "\nLOAD CSV WITH HEADERS FROM 'file:///" + filename + "' AS row\n" + "WITH"

    for col, type, in columns.items():
        if "int" in str(type):
            load += "\n\ttoInteger(row." + col + ") AS " + col + ","
        else:
            load += "\n\trow." + col + " AS " + col + ","

    load = load.removesuffix(",") + "\nMERGE (s:Submission {"

    for col in columns.index:
        load += "\n\t" + col + ": " + col + ","

    load = load.removesuffix(",") + "\n})"
    return load


def getConstraintQuery():
    return "// Unique Submission constraint\n" + \
        "CREATE CONSTRAINT UniqueSubmission IF NOT EXISTS\n" + \
        "FOR (s:Submission)\n" + \
        "REQUIRE s._id IS UNIQUE\n"

def getRelationsQuery():
    return "// Derives relation\n" + \
    "MATCH (s:Submission)\n" + \
    "MATCH (d:Submission)\n" + \
    "WHERE s._id = d.derivationOf AND s._id <> d._id\n" + \
    "MERGE (d)-[r:Derives]->(s)\n" + \
    "RETURN count(r)"


def getSetLabelQuery(sat: int, label: str):
    return "// Set " + label + " solutions\n" + \
    "MATCH (s:Submission {sat: " + str(sat) + "})\n" + \
    "SET s:" + label + \
    "\nRETURN count(s)" 


def getCorrectQuery():
    return getSetLabelQuery(1, "Correct")


def getIncorrectQuery():
    return getSetLabelQuery(0, "Incorrect")


def importData():
    # Import all the data from a directory
    # return: a dictionary of dataframes
    dir = "data"
    dict = {}
    dataTypes = {"_id": str, "cmd_n": str, "code": str,
                 "derivationOf": str, "sat": int, "expr": str, "ast": str}

    for subdir in os.listdir(dir):
        dict[subdir] = {}
        subdirPath = os.path.join(dir, subdir)
        for file in os.listdir(subdirPath):
            if file.endswith(".csv"):
                name = file.removesuffix(".csv")
                path = os.path.join(dir, subdir, file)
                dict[subdir][name] = pd.read_csv(path, dtype=dataTypes)

    return dict


def main():
    # Import files from the data directory
    dict = importData()

    challenge = "9jPK8KBWzjFmBx4Hb"
    pred = "prop1"

    file = open("queries.txt", "w")
    file.write(getConstraintQuery() + "\n\n")
    file.write(getLoadQuery(challenge, pred, dict) + "\n\n")
    file.write(getRelationsQuery() + "\n\n")
    file.write(getCorrectQuery() + "\n\n")
    file.write(getIncorrectQuery() + "\n\n")
    file.close()

    print("Cretaed queries.txt file.")


if __name__ == "__main__":
    main()
