import pandas as pd
import os


def getLoadQuery(file, columns):

    commonColumns = ["cmd_i", "sat", "_id", "code", "derivationOf"]
    columns = [col for col in columns if col not in commonColumns]

    load = "\n//Load gAeD3MTGCCv8YNTaK\nLOAD CSV WITH HEADERS FROM 'file:///" + file + \
        ".csv' AS row\n" + \
        "WITH toInteger(row.cmd_i) AS cmd_i, \n\ttoInteger(row.sat) AS sat," + \
        "row._id AS id, \n\trow.code AS code, \n\trow.derivationOf AS derivationOf"

    for col in columns:
        load += ",\n\trow." + col + " AS " + col

    load += "\nMERGE (s:Submission {" + \
        "\n\tid: id," + \
        "\n\tcmd_i: cmd_i," + \
        "\n\tcode: code," + \
        "\n\tsat: sat," + \
        "\n\tderivationOf: derivationOf"

    for col in columns:
        load += ",\n\t" + col + ": " + col

    load += "\n})"

    return load


def importDataFromDir(dir):
    # Import all the data from a directory
    # dir: directory with the data
    # return: a dictionary of dataframes
    dict = {}

    for file in os.listdir(dir):
        if file.endswith('.csv'):
            df = pd.read_csv(f'{dir}/' + file)
            dict[file.removesuffix(".csv")] = df
            print(f"Imported {file}.")

    return dict


# Import files from the data directory
dict = importDataFromDir('../data_preparation/data/prepared')

file = "9jPK8KBWzjFmBx4Hb"
query = getLoadQuery(file, dict[file].columns)
print(query)
