#!/usr/bin/python2.7
#coding=utf8
r'''
Fuction: Download 3rd-party from remote
Created: Tuyj
Created date:2017/03/09
'''
import argparse
import os, shutil, platform
import json

def check_repository(cfg):
    try:
        path = ""
        repository = cfg['repository']
        if platform.system().startswith("Windows"):
            path = repository['path_win']
        else:
            path = repository['path_linux']
        msg = repository['failure_msg']
    except KeyError as ex:
        print "!!!!!!!!!!!Cfg file lost some items: %s" % ex.message
        return None
    if not os.path.exists(path) or not os.path.isdir(path):
        print "!!!!!!!!!!!Repository path[%s] is not existed or invalid" % path
        print "!!!!!!!!!!!Maybe you need to do: %s" % msg
        return None
    return path

def copyDep(src_dir, out_dir, type, src_list):
    if type not in ["dir", "file"]:
        print "Unknow type:", type
        return
    for sub in src_list:
        sub_src = os.path.join(src_dir, os.path.join(*sub))
        sub_out = os.path.join(out_dir, os.path.join(*sub))
        if type == "dir":
            copyFunc = shutil.copytree
        elif type == "file":
            copyFunc = shutil.copyfile
            ssub_dir = os.path.dirname(sub_out)
            if not os.path.exists(ssub_dir):
                os.makedirs(ssub_dir)
        copyFunc(sub_src, sub_out)

def writeCopyFrom(src_dir, out_dir):
    try:
        copyfrom = os.path.join(out_dir, "CopyFrom")
        message = "Copy from: %s to %s" % (src_dir, copyfrom)
        fp = open(copyfrom, "w")
        fp.writelines([message])
        fp.close()
        print "-----------", message
    except Exception as ex:
        print "!!!!!!!!!!!Write CopyFrom to %s failed: %s" % (out_dir, ex.message)

def gen_android(cfg, out_dir):
    repository_path = check_repository(cfg)
    if repository_path is None:
        exit(2)
    try:
        android_cfg = cfg['android']
        git_version = android_cfg['git-version']
        deps = android_cfg['deps']
        targets = android_cfg['targets']
    except KeyError as ex:
        print "!!!!!!!!!!!Cfg file lost some items in linux: %s" % ex.message
        return None
    
    src_dir = os.path.join(repository_path, git_version, 'android')
    for target,status in targets.items():
        if status != 'true':
            continue
        
        print '-----------Copy target %s' % target
        target_src_dir = os.path.join(src_dir, target)
        target_out_dir = os.path.join(out_dir, target)
        if not os.path.exists(target_out_dir):
            os.makedirs(target_out_dir)
        for type,src_list in deps.items():
            copyDep(target_src_dir, target_out_dir, type, src_list)

    writeCopyFrom(src_dir, out_dir)

def loadCfg(cfg_file):
    if not os.path.exists(cfg_file) or os.path.isdir(cfg_file):
        print "!!!!!!!!!!!Configured file[%s] is not existed or invalid" % cfg_file
        return False,None
    try:
        cfg = {}
        fp = open(cfg_file, 'r')
        cfg = json.load(fp)
        fp.close()
    except Exception as ex:
        if fp is not None: fp.close()
        print '!!!!!!!!!!!Json is invalid in configured file[%s] %s' % (cfg_file, ex.message)
        return False,None
    return True,cfg

class myArgumentParser(argparse.ArgumentParser):
    def __init__(self, eg='', *args, **kwargs):
        argparse.ArgumentParser.__init__(self, *args, **kwargs)
        self.eg = eg
    def print_usage(self, file=None):
        argparse.ArgumentParser.print_usage(self, file)
        self._print_message('    e.g.: %s\n' % self.eg, file)

if __name__ == '__main__':
    parser = myArgumentParser('python2.7 _gen_deps.py -c _deps.json -o "<Project>/src/main/cpp/avalon"')
    parser.add_argument('-c', '--cfg-file', action='store', dest='cfg_file',
            required=True,
            help='configured file in json format')
    parser.add_argument('-o', '--out-dir', action='store', dest='out_dir',
            required=True,
            help='absolute out put directory(avalon)')
    results = parser.parse_args()
    
    if os.path.exists(results.out_dir):
        if not os.path.isdir(results.out_dir):
            print "!!!!!!!!!!!Out-put directory[%s] is invalid" % results.out_dir
            exit(2)
        shutil.rmtree(results.out_dir)
        print "-----------ReCreate", results.out_dir
    os.mkdir(results.out_dir)
    
    ret,cfg = loadCfg(results.cfg_file)
    if not ret:
        exit(2)
    
    gen_android(cfg, results.out_dir)



   