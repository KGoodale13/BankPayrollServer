!#/bin/bash

# MongoDB auth variables
user=
pass=
authDb=admin
filename=


usage()
{
    echo "usage: dataimport [-u user] [-p password] [-d authDb] [-f file]"
}

while [ "$1" != "" ]; do
    case $1 in
        -u | --user )           shift
                                user=$1
                                ;;
        -p | --password )       shift
                                password=$1
                                ;;
        -f | --file )           shift
                                filename=$1
                                ;;
        -d | --authDb )         shift
                                authDb=$1
                                ;;
        * )                     usage
                                exit 1
    esac
    shift
done

# Replace the current collection with the new dumped one
mongoimport -u dataimport -p $pass --authenticationDatabase $authDb --db cos420 --collection companies --file $file --drop --jsonArray

# Re add our authorizedEmails index for performance since that is the index we use for lookups most often
mongo cos420 -u dataimport -p $pass --authenticationDatabase $authDb --eval "db.companies.createIndex( { authorizedEmails: 1 } );"