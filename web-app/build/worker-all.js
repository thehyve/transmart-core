(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);throw new Error("Cannot find module '"+o+"'")}var f=n[o]={exports:{}};t[o][0].call(f.exports,function(e){var n=t[o][1][e];return s(n?n:e)},f,f.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2011
//
// bam.js: indexed binary alignments
//

"use strict";

if (typeof(require) !== 'undefined') {
    var spans = require('./spans');
    var Range = spans.Range;
    var union = spans.union;
    var intersection = spans.intersection;

    var bin = require('./bin');
    var readInt = bin.readInt;
    var readShort = bin.readShort;
    var readByte = bin.readByte;
    var readInt64 = bin.readInt64;
    var readFloat = bin.readFloat;

    var lh3utils = require('./lh3utils');
    var readVob = lh3utils.readVob;
    var unbgzf = lh3utils.unbgzf;
    var reg2bins = lh3utils.reg2bins;
    var Chunk = lh3utils.Chunk;
}


var BAM_MAGIC = 0x14d4142;
var BAI_MAGIC = 0x1494142;

var BamFlags = {
    MULTIPLE_SEGMENTS:       0x1,
    ALL_SEGMENTS_ALIGN:      0x2,
    SEGMENT_UNMAPPED:        0x4,
    NEXT_SEGMENT_UNMAPPED:   0x8,
    REVERSE_COMPLEMENT:      0x10,
    NEXT_REVERSE_COMPLEMENT: 0x20,
    FIRST_SEGMENT:           0x40,
    LAST_SEGMENT:            0x80,
    SECONDARY_ALIGNMENT:     0x100,
    QC_FAIL:                 0x200,
    DUPLICATE:               0x400,
    SUPPLEMENTARY:           0x800
};

function BamFile() {
}

function makeBam(data, bai, callback) {
    var bam = new BamFile();
    bam.data = data;
    bam.bai = bai;

    bam.bai.fetch(function(header) {   // Do we really need to fetch the whole thing? :-(
        if (!header) {
            return callback(null, "Couldn't access BAI");
        }

        var uncba = new Uint8Array(header);
        var baiMagic = readInt(uncba, 0);
        if (baiMagic != BAI_MAGIC) {
            return callback(null, 'Not a BAI file, magic=0x' + baiMagic.toString(16));
        }

        var nref = readInt(uncba, 4);

        bam.indices = [];

        var p = 8;
        var minBlockIndex = 1000000000;
        for (var ref = 0; ref < nref; ++ref) {
            var blockStart = p;
            var nbin = readInt(uncba, p); p += 4;
            for (var b = 0; b < nbin; ++b) {
                var bin = readInt(uncba, p);
                var nchnk = readInt(uncba, p+4);
                p += 8 + (nchnk * 16);
            }
            var nintv = readInt(uncba, p); p += 4;
            
            var q = p;
            for (var i = 0; i < nintv; ++i) {
                var v = readVob(uncba, q); q += 8;
                if (v) {
                    var bi = v.block;
                    if (v.offset > 0)
                        bi += 65536;

                    if (bi < minBlockIndex)
                        minBlockIndex = bi;
                    break;
                }
            }
            p += (nintv * 8);


            if (nbin > 0) {
                bam.indices[ref] = new Uint8Array(header, blockStart, p - blockStart);
            }                     
        }

        bam.data.slice(0, minBlockIndex).fetch(function(r) {
            if (!r) {
                return callback(null, "Couldn't access BAM");
            }
            
            var unc = unbgzf(r, r.byteLength);
            var uncba = new Uint8Array(unc);

            var magic = readInt(uncba, 0);
            if (magic != BAM_MAGIC) {
                return callback(null, "Not a BAM file, magic=0x" + magic.toString(16));
            }
            var headLen = readInt(uncba, 4);
            var header = '';
            for (var i = 0; i < headLen; ++i) {
                header += String.fromCharCode(uncba[i + 8]);
            }

            var nRef = readInt(uncba, headLen + 8);
            var p = headLen + 12;

            bam.chrToIndex = {};
            bam.indexToChr = [];
            for (var i = 0; i < nRef; ++i) {
                var lName = readInt(uncba, p);
                var name = '';
                for (var j = 0; j < lName-1; ++j) {
                    name += String.fromCharCode(uncba[p + 4 + j]);
                }
                var lRef = readInt(uncba, p + lName + 4);
                bam.chrToIndex[name] = i;
                if (name.indexOf('chr') == 0) {
                    bam.chrToIndex[name.substring(3)] = i;
                } else {
                    bam.chrToIndex['chr' + name] = i;
                }
                bam.indexToChr.push(name);

                p = p + 8 + lName;
            }

            if (bam.indices) {
                return callback(bam);
            }
        });
    });
}



BamFile.prototype.blocksForRange = function(refId, min, max) {
    var index = this.indices[refId];
    if (!index) {
        return [];
    }

    var intBinsL = reg2bins(min, max);
    var intBins = [];
    for (var i = 0; i < intBinsL.length; ++i) {
        intBins[intBinsL[i]] = true;
    }
    var leafChunks = [], otherChunks = [];

    var nbin = readInt(index, 0);
    var p = 4;
    for (var b = 0; b < nbin; ++b) {
        var bin = readInt(index, p);
        var nchnk = readInt(index, p+4);
//        dlog('bin=' + bin + '; nchnk=' + nchnk);
        p += 8;
        if (intBins[bin]) {
            for (var c = 0; c < nchnk; ++c) {
                var cs = readVob(index, p);
                var ce = readVob(index, p + 8);
                (bin < 4681 ? otherChunks : leafChunks).push(new Chunk(cs, ce));
                p += 16;
            }
        } else {
            p +=  (nchnk * 16);
        }
    }
//    dlog('leafChunks = ' + miniJSONify(leafChunks));
//    dlog('otherChunks = ' + miniJSONify(otherChunks));

    var nintv = readInt(index, p);
    var lowest = null;
    var minLin = Math.min(min>>14, nintv - 1), maxLin = Math.min(max>>14, nintv - 1);
    for (var i = minLin; i <= maxLin; ++i) {
        var lb =  readVob(index, p + 4 + (i * 8));
        if (!lb) {
            continue;
        }
        if (!lowest || lb.block < lowest.block || lb.offset < lowest.offset) {
            lowest = lb;
        }
    }
    // dlog('Lowest LB = ' + lowest);
    
    var prunedOtherChunks = [];
    if (lowest != null) {
        for (var i = 0; i < otherChunks.length; ++i) {
            var chnk = otherChunks[i];
            if (chnk.maxv.block >= lowest.block && chnk.maxv.offset >= lowest.offset) {
                prunedOtherChunks.push(chnk);
            }
        }
    }
    // dlog('prunedOtherChunks = ' + miniJSONify(prunedOtherChunks));
    otherChunks = prunedOtherChunks;

    var intChunks = [];
    for (var i = 0; i < otherChunks.length; ++i) {
        intChunks.push(otherChunks[i]);
    }
    for (var i = 0; i < leafChunks.length; ++i) {
        intChunks.push(leafChunks[i]);
    }

    intChunks.sort(function(c0, c1) {
        var dif = c0.minv.block - c1.minv.block;
        if (dif != 0) {
            return dif;
        } else {
            return c0.minv.offset - c1.minv.offset;
        }
    });
    var mergedChunks = [];
    if (intChunks.length > 0) {
        var cur = intChunks[0];
        for (var i = 1; i < intChunks.length; ++i) {
            var nc = intChunks[i];
            if (nc.minv.block == cur.maxv.block /* && nc.minv.offset == cur.maxv.offset */) { // no point splitting mid-block
                cur = new Chunk(cur.minv, nc.maxv);
            } else {
                mergedChunks.push(cur);
                cur = nc;
            }
        }
        mergedChunks.push(cur);
    }
    // dlog('mergedChunks = ' + miniJSONify(mergedChunks));

    return mergedChunks;
}

BamFile.prototype.fetch = function(chr, min, max, callback, opts) {
    var thisB = this;
    opts = opts || {};

    var chrId = this.chrToIndex[chr];
    var chunks;
    if (chrId === undefined) {
        chunks = [];
    } else {
        chunks = this.blocksForRange(chrId, min, max);
        if (!chunks) {
            callback(null, 'Error in index fetch');
        }
    }
    
    var records = [];
    var index = 0;
    var data;

    function tramp() {
        if (index >= chunks.length) {
            return callback(records);
        } else if (!data) {
            // dlog('fetching ' + index);
            var c = chunks[index];
            var fetchMin = c.minv.block;
            var fetchMax = c.maxv.block + (1<<16); // *sigh*
            thisB.data.slice(fetchMin, fetchMax - fetchMin).fetch(function(r) {
                data = unbgzf(r, c.maxv.block - c.minv.block + 1);
                return tramp();
            });
        } else {
            var ba = new Uint8Array(data);
            thisB.readBamRecords(ba, chunks[index].minv.offset, records, min, max, chrId, opts);
            data = null;
            ++index;
            return tramp();
        }
    }
    tramp();
}

var SEQRET_DECODER = ['=', 'A', 'C', 'x', 'G', 'x', 'x', 'x', 'T', 'x', 'x', 'x', 'x', 'x', 'x', 'N'];
var CIGAR_DECODER = ['M', 'I', 'D', 'N', 'S', 'H', 'P', '=', 'X', '?', '?', '?', '?', '?', '?', '?'];

function BamRecord() {
}

BamFile.prototype.readBamRecords = function(ba, offset, sink, min, max, chrId, opts) {
    while (true) {
        var blockSize = readInt(ba, offset);
        var blockEnd = offset + blockSize + 4;
        if (blockEnd >= ba.length) {
            return sink;
        }

        var record = new BamRecord();

        var refID = readInt(ba, offset + 4);
        var pos = readInt(ba, offset + 8);
        
        var bmn = readInt(ba, offset + 12);
        var bin = (bmn & 0xffff0000) >> 16;
        var mq = (bmn & 0xff00) >> 8;
        var nl = bmn & 0xff;

        var flag_nc = readInt(ba, offset + 16);
        var flag = (flag_nc & 0xffff0000) >> 16;
        var nc = flag_nc & 0xffff;
    
        var lseq = readInt(ba, offset + 20);
        
        var nextRef  = readInt(ba, offset + 24);
        var nextPos = readInt(ba, offset + 28);
        
        var tlen = readInt(ba, offset + 32);
    
        record.segment = this.indexToChr[refID];
        record.flag = flag;
        record.pos = pos;
        record.mq = mq;
        if (opts.light)
            record.seqLength = lseq;

        if (!opts.light) {
            if (nextRef >= 0) {
                record.nextSegment = this.indexToChr[nextRef];
                record.nextPos = nextPos;
            }

            var readName = '';
            for (var j = 0; j < nl-1; ++j) {
                readName += String.fromCharCode(ba[offset + 36 + j]);
            }
            record.readName = readName;
        
            var p = offset + 36 + nl;

            var cigar = '';
            for (var c = 0; c < nc; ++c) {
                var cigop = readInt(ba, p);
                cigar = cigar + (cigop>>4) + CIGAR_DECODER[cigop & 0xf];
                p += 4;
            }
            record.cigar = cigar;
        
            var seq = '';
            var seqBytes = (lseq + 1) >> 1;
            for (var j = 0; j < seqBytes; ++j) {
                var sb = ba[p + j];
                seq += SEQRET_DECODER[(sb & 0xf0) >> 4];
                seq += SEQRET_DECODER[(sb & 0x0f)];
            }
            p += seqBytes;
            record.seq = seq;

            var qseq = '';
            for (var j = 0; j < lseq; ++j) {
                qseq += String.fromCharCode(ba[p + j] + 33);
            }
            p += lseq;
            record.quals = qseq;

            while (p < blockEnd) {
                var tag = String.fromCharCode(ba[p], ba[p + 1]);
                var type = String.fromCharCode(ba[p + 2]);
                var value;

                if (type == 'A') {
                    value = String.fromCharCode(ba[p + 3]);
                    p += 4;
                } else if (type == 'i' || type == 'I') {
                    value = readInt(ba, p + 3);
                    p += 7;
                } else if (type == 'c' || type == 'C') {
                    value = ba[p + 3];
                    p += 4;
                } else if (type == 's' || type == 'S') {
                    value = readShort(ba, p + 3);
                    p += 5;
                } else if (type == 'f') {
                    value = readFloat(ba, p + 3);
                    p += 7;
                } else if (type == 'Z' || type == 'H') {
                    p += 3;
                    value = '';
                    for (;;) {
                        var cc = ba[p++];
                        if (cc == 0) {
                            break;
                        } else {
                            value += String.fromCharCode(cc);
                        }
                    }
                } else if (type == 'B') {
                    var atype = String.fromCharCode(ba[p + 3]);
                    var alen = readInt(ba, p + 4);
                    var elen;
                    var reader;
                    if (atype == 'i' || atype == 'I' || atype == 'f') {
                        elen = 4;
                        if (atype == 'f')
                            reader = readFloat;
                        else
                            reader = readInt;
                    } else if (atype == 's' || atype == 'S') {
                        elen = 2;
                        reader = readShort;
                    } else if (atype == 'c' || atype == 'C') {
                        elen = 1;
                        reader = readByte;
                    } else {
                        throw 'Unknown array type ' + atype;
                    }

                    p += 8;
                    value = [];
                    for (var i = 0; i < alen; ++i) {
                        value.push(reader(ba, p));
                        p += elen;
                    }
                } else {
                    throw 'Unknown type '+ type;
                }
                record[tag] = value;
            }
        }

        if (!min || record.pos <= max && record.pos + lseq >= min) {
            if (chrId === undefined || refID == chrId) {
                sink.push(record);
            }
        }
        offset = blockEnd;
    }

    // Exits via top of loop.
};

if (typeof(module) !== 'undefined') {
    module.exports = {
        makeBam: makeBam,
        BAM_MAGIC: BAM_MAGIC,
        BAI_MAGIC: BAI_MAGIC,
        BamFlags: BamFlags
    };
}
},{"./bin":3,"./lh3utils":7,"./spans":9}],2:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2010
//
// bigwig.js: indexed binary WIG (and BED) files
//

"use strict";


if (typeof(require) !== 'undefined') {
    var spans = require('./spans');
    var Range = spans.Range;
    var union = spans.union;
    var intersection = spans.intersection;

    var das = require('./das');
    var DASFeature = das.DASFeature;
    var DASGroup = das.DASGroup;

    var utils = require('./utils');
    var shallowCopy = utils.shallowCopy;

    var bin = require('./bin');
    var readInt = bin.readInt;

    var jszlib = require('jszlib');
    var jszlib_inflate_buffer = jszlib.inflateBuffer;
    var arrayCopy = jszlib.arrayCopy;
}

var BIG_WIG_MAGIC = 0x888FFC26;
var BIG_WIG_MAGIC_BE = 0x26FC8F88;
var BIG_BED_MAGIC = 0x8789F2EB;
var BIG_BED_MAGIC_BE = 0xEBF28987;


var BIG_WIG_TYPE_GRAPH = 1;
var BIG_WIG_TYPE_VSTEP = 2;
var BIG_WIG_TYPE_FSTEP = 3;
  
var M1 = 256;
var M2 = 256*256;
var M3 = 256*256*256;
var M4 = 256*256*256*256;

var BED_COLOR_REGEXP = new RegExp("^[0-9]+,[0-9]+,[0-9]+");

function bwg_readOffset(ba, o) {
    var offset = ba[o] + ba[o+1]*M1 + ba[o+2]*M2 + ba[o+3]*M3 + ba[o+4]*M4;
    return offset;
}

function BigWig() {
}

BigWig.prototype.readChromTree = function(callback) {
    var thisB = this;
    this.chromsToIDs = {};
    this.idsToChroms = {};
    this.maxID = 0;

    var udo = this.unzoomedDataOffset;
    var eb = (udo - this.chromTreeOffset) & 3;
    udo = udo + 4 - eb;

    this.data.slice(this.chromTreeOffset, udo - this.chromTreeOffset).fetch(function(bpt) {
        var ba = new Uint8Array(bpt);
        var sa = new Int16Array(bpt);
        var la = new Int32Array(bpt);
        var bptMagic = la[0];
        var blockSize = la[1];
        var keySize = la[2];
        var valSize = la[3];
        var itemCount = bwg_readOffset(ba, 16);
        var rootNodeOffset = 32;

        var bptReadNode = function(offset) {
            var nodeType = ba[offset];
            var cnt = sa[(offset/2) + 1];
            offset += 4;
            for (var n = 0; n < cnt; ++n) {
                if (nodeType == 0) {
                    offset += keySize;
                    var childOffset = bwg_readOffset(ba, offset);
                    offset += 8;
                    childOffset -= thisB.chromTreeOffset;
                    bptReadNode(childOffset);
                } else {
                    var key = '';
                    for (var ki = 0; ki < keySize; ++ki) {
                        var charCode = ba[offset++];
                        if (charCode != 0) {
                            key += String.fromCharCode(charCode);
                        }
                    }
                    var chromId = (ba[offset+3]<<24) | (ba[offset+2]<<16) | (ba[offset+1]<<8) | (ba[offset+0]);
                    var chromSize = (ba[offset + 7]<<24) | (ba[offset+6]<<16) | (ba[offset+5]<<8) | (ba[offset+4]);
                    offset += 8;

                    thisB.chromsToIDs[key] = chromId;
                    if (key.indexOf('chr') == 0) {
                        thisB.chromsToIDs[key.substr(3)] = chromId;
                    }
                    thisB.idsToChroms[chromId] = key;
                    thisB.maxID = Math.max(thisB.maxID, chromId);
                }
            }
        };
        bptReadNode(rootNodeOffset);

        callback(thisB);
    });
}

function BigWigView(bwg, cirTreeOffset, cirTreeLength, isSummary) {
    this.bwg = bwg;
    this.cirTreeOffset = cirTreeOffset;
    this.cirTreeLength = cirTreeLength;
    this.isSummary = isSummary;
}



BigWigView.prototype.readWigData = function(chrName, min, max, callback) {
    var chr = this.bwg.chromsToIDs[chrName];
    if (chr === undefined) {
        // Not an error because some .bwgs won't have data for all chromosomes.

        // dlog("Couldn't find chr " + chrName);
        // dlog('Chroms=' + miniJSONify(this.bwg.chromsToIDs));
        return callback([]);
    } else {
        this.readWigDataById(chr, min, max, callback);
    }
}

BigWigView.prototype.readWigDataById = function(chr, min, max, callback) {
    var thisB = this;
    if (!this.cirHeader) {
        this.bwg.data.slice(this.cirTreeOffset, 48).fetch(function(result) {
            thisB.cirHeader = result;
            var la = new Int32Array(thisB.cirHeader);
            thisB.cirBlockSize = la[1];
            thisB.readWigDataById(chr, min, max, callback);
        });
        return;
    }

    var blocksToFetch = [];
    var outstanding = 0;

    var beforeBWG = Date.now();

    var filter = function(chromId, fmin, fmax, toks) {
        return ((chr < 0 || chromId == chr) && fmin <= max && fmax >= min);
    }

    var cirFobRecur = function(offset, level) {
        if (thisB.bwg.instrument)
            console.log('level=' + level + '; offset=' + offset + '; time=' + (Date.now()|0));

        outstanding += offset.length;

        if (offset.length == 1 && offset[0] - thisB.cirTreeOffset == 48 && thisB.cachedCirRoot) {
            cirFobRecur2(thisB.cachedCirRoot, 0, level);
            --outstanding;
            if (outstanding == 0) {
                thisB.fetchFeatures(filter, blocksToFetch, callback);
            }
            return;
        }

        var maxCirBlockSpan = 4 +  (thisB.cirBlockSize * 32);   // Upper bound on size, based on a completely full leaf node.
        var spans;
        for (var i = 0; i < offset.length; ++i) {
            var blockSpan = new Range(offset[i], offset[i] + maxCirBlockSpan);
            spans = spans ? union(spans, blockSpan) : blockSpan;
        }
        
        var fetchRanges = spans.ranges();
        for (var r = 0; r < fetchRanges.length; ++r) {
            var fr = fetchRanges[r];
            cirFobStartFetch(offset, fr, level);
        }
    }

    var cirFobStartFetch = function(offset, fr, level, attempts) {
        var length = fr.max() - fr.min();
        thisB.bwg.data.slice(fr.min(), fr.max() - fr.min()).fetch(function(resultBuffer) {
            for (var i = 0; i < offset.length; ++i) {
                if (fr.contains(offset[i])) {
                    cirFobRecur2(resultBuffer, offset[i] - fr.min(), level);

                    if (offset[i] - thisB.cirTreeOffset == 48 && offset[i] - fr.min() == 0)
                        thisB.cachedCirRoot = resultBuffer;

                    --outstanding;
                    if (outstanding == 0) {
                        thisB.fetchFeatures(filter, blocksToFetch, callback);
                    }
                }
            }
        });
    }

    var cirFobRecur2 = function(cirBlockData, offset, level) {
        var ba = new Uint8Array(cirBlockData);
        var sa = new Int16Array(cirBlockData);
        var la = new Int32Array(cirBlockData);

        var isLeaf = ba[offset];
        var cnt = sa[offset/2 + 1];
        offset += 4;

        if (isLeaf != 0) {
            for (var i = 0; i < cnt; ++i) {
                var lo = offset/4;
                var startChrom = la[lo];
                var startBase = la[lo + 1];
                var endChrom = la[lo + 2];
                var endBase = la[lo + 3];
                var blockOffset = bwg_readOffset(ba, offset+16);
                var blockSize = bwg_readOffset(ba, offset+24);
                if (((chr < 0 || startChrom < chr) || (startChrom == chr && startBase <= max)) &&
                    ((chr < 0 || endChrom   > chr) || (endChrom == chr && endBase >= min)))
                {
                    blocksToFetch.push({offset: blockOffset, size: blockSize});
                }
                offset += 32;
            }
        } else {
            var recurOffsets = [];
            for (var i = 0; i < cnt; ++i) {
                var lo = offset/4;
                var startChrom = la[lo];
                var startBase = la[lo + 1];
                var endChrom = la[lo + 2];
                var endBase = la[lo + 3];
                var blockOffset = bwg_readOffset(ba, offset+16);
                if ((chr < 0 || startChrom < chr || (startChrom == chr && startBase <= max)) &&
                    (chr < 0 || endChrom   > chr || (endChrom == chr && endBase >= min)))
                {
                    recurOffsets.push(blockOffset);
                }
                offset += 24;
            }
            if (recurOffsets.length > 0) {
                cirFobRecur(recurOffsets, level + 1);
            }
        }
    };

    cirFobRecur([thisB.cirTreeOffset + 48], 1);
}


BigWigView.prototype.fetchFeatures = function(filter, blocksToFetch, callback) {
    var thisB = this;

    blocksToFetch.sort(function(b0, b1) {
        return (b0.offset|0) - (b1.offset|0);
    });

    if (blocksToFetch.length == 0) {
        callback([]);
    } else {
        var features = [];
        var createFeature = function(chr, fmin, fmax, opts) {
            if (!opts) {
                opts = {};
            }
        
            var f = new DASFeature();
            f._chromId = chr;
            f.segment = thisB.bwg.idsToChroms[chr];
            f.min = fmin;
            f.max = fmax;
            f.type = 'bigwig';
            
            for (var k in opts) {
                f[k] = opts[k];
            }
            
            features.push(f);
        };

        var tramp = function() {
            if (blocksToFetch.length == 0) {
                var afterBWG = Date.now();
                // dlog('BWG fetch took ' + (afterBWG - beforeBWG) + 'ms');
                callback(features);
                return;  // just in case...
            } else {
                var block = blocksToFetch[0];
                if (block.data) {
                    thisB.parseFeatures(block.data, createFeature, filter);
                    blocksToFetch.splice(0, 1);
                    tramp();
                } else {
                    var fetchStart = block.offset;
                    var fetchSize = block.size;
                    var bi = 1;
                    while (bi < blocksToFetch.length && blocksToFetch[bi].offset == (fetchStart + fetchSize)) {
                        fetchSize += blocksToFetch[bi].size;
                        ++bi;
                    }

                    thisB.bwg.data.slice(fetchStart, fetchSize).fetch(function(result) {
                        var offset = 0;
                        var bi = 0;
                        while (offset < fetchSize) {
                            var fb = blocksToFetch[bi];
                        
                            var data;
                            if (thisB.bwg.uncompressBufSize > 0) {
                                data = jszlib_inflate_buffer(result, offset + 2, fb.size - 2);
                            } else {
                                var tmp = new Uint8Array(fb.size);    // FIXME is this really the best we can do?
                                arrayCopy(new Uint8Array(result, offset, fb.size), 0, tmp, 0, fb.size);
                                data = tmp.buffer;
                            }
                            fb.data = data;
                            
                            offset += fb.size;
                            ++bi;
                        }
                        tramp();
                    });
                }
            }
        }
        tramp();
    }
}

BigWigView.prototype.parseFeatures = function(data, createFeature, filter) {
    var ba = new Uint8Array(data);

    if (this.isSummary) {
        var sa = new Int16Array(data);
        var la = new Int32Array(data);
        var fa = new Float32Array(data);

        var itemCount = data.byteLength/32;
        for (var i = 0; i < itemCount; ++i) {
            var chromId =   la[(i*8)];
            var start =     la[(i*8)+1];
            var end =       la[(i*8)+2];
            var validCnt =  la[(i*8)+3];
            var minVal    = fa[(i*8)+4];
            var maxVal    = fa[(i*8)+5];
            var sumData   = fa[(i*8)+6];
            var sumSqData = fa[(i*8)+7];
            
            if (filter(chromId, start + 1, end)) {
                var summaryOpts = {type: 'bigwig', score: sumData/validCnt, maxScore: maxVal};
                if (this.bwg.type == 'bigbed') {
                    summaryOpts.type = 'density';
                }
                createFeature(chromId, start + 1, end, summaryOpts);
            }
        }
    } else if (this.bwg.type == 'bigwig') {
        var sa = new Int16Array(data);
        var la = new Int32Array(data);
        var fa = new Float32Array(data);

        var chromId = la[0];
        var blockStart = la[1];
        var blockEnd = la[2];
        var itemStep = la[3];
        var itemSpan = la[4];
        var blockType = ba[20];
        var itemCount = sa[11];
        
        if (blockType == BIG_WIG_TYPE_FSTEP) {
            for (var i = 0; i < itemCount; ++i) {
                var score = fa[i + 6];
                var fmin = blockStart + (i*itemStep) + 1, fmax = blockStart + (i*itemStep) + itemSpan;
                if (filter(chromId, fmin, fmax))
                    createFeature(chromId, fmin, fmax, {score: score});
            }
        } else if (blockType == BIG_WIG_TYPE_VSTEP) {
            for (var i = 0; i < itemCount; ++i) {
                var start = la[(i*2) + 6] + 1;
                var end = start + itemSpan - 1;
                var score = fa[(i*2) + 7];
                if (filter(chromId, start, end))
                    createFeature(chromId, start, end, {score: score});
            }
        } else if (blockType == BIG_WIG_TYPE_GRAPH) {
            for (var i = 0; i < itemCount; ++i) {
                var start = la[(i*3) + 6] + 1;
                var end   = la[(i*3) + 7];
                var score = fa[(i*3) + 8];
                if (start > end) {
                    start = end;
                }
                if (filter(chromId, start, end))
                    createFeature(chromId, start, end, {score: score});
            }
        } else {
            console.log('Currently not handling bwgType=' + blockType);
        }
    } else if (this.bwg.type == 'bigbed') {
        var offset = 0;
        var dfc = this.bwg.definedFieldCount;
        var schema = this.bwg.schema;

        while (offset < ba.length) {
            var chromId = (ba[offset+3]<<24) | (ba[offset+2]<<16) | (ba[offset+1]<<8) | (ba[offset+0]);
            var start = (ba[offset+7]<<24) | (ba[offset+6]<<16) | (ba[offset+5]<<8) | (ba[offset+4]);
            var end = (ba[offset+11]<<24) | (ba[offset+10]<<16) | (ba[offset+9]<<8) | (ba[offset+8]);
            offset += 12;
            var rest = '';
            while (true) {
                var ch = ba[offset++];
                if (ch != 0) {
                    rest += String.fromCharCode(ch);
                } else {
                    break;
                }
            }

            var featureOpts = {};
            
            var bedColumns;
            if (rest.length > 0) {
                bedColumns = rest.split('\t');
            } else {
                bedColumns = [];
            }
            if (bedColumns.length > 0 && dfc > 3) {
                featureOpts.label = bedColumns[0];
            }
            if (bedColumns.length > 1 && dfc > 4) {
                var score = parseInt(bedColumns[1]);
                if (!isNaN(score))
                    featureOpts.score = score;
            }
            if (bedColumns.length > 2 && dfc > 5) {
                featureOpts.orientation = bedColumns[2];
            }
            if (bedColumns.length > 5 && dfc > 8) {
                var color = bedColumns[5];
                if (BED_COLOR_REGEXP.test(color)) {
                    featureOpts.itemRgb = 'rgb(' + color + ')';
                }
            }

            if (bedColumns.length > dfc-3 && schema) {
                for (var col = dfc - 3; col < bedColumns.length; ++col) {
                    featureOpts[schema.fields[col+3].name] = bedColumns[col];
                }
            }

            if (filter(chromId, start + 1, end, bedColumns)) {
                if (dfc < 12) {
                    createFeature(chromId, start + 1, end, featureOpts);
                } else {
                    var thickStart = bedColumns[3]|0;
                    var thickEnd   = bedColumns[4]|0;
                    var blockCount = bedColumns[6]|0;
                    var blockSizes = bedColumns[7].split(',');
                    var blockStarts = bedColumns[8].split(',');
                    
                    featureOpts.type = 'transcript'
                    var grp = new DASGroup();
                    for (var k in featureOpts) {
                        grp[k] = featureOpts[k];
                    }
                    grp.id = bedColumns[0];
                    grp.segment = this.bwg.idsToChroms[chromId];
                    grp.min = start + 1;
                    grp.max = end;
                    grp.notes = [];
                    featureOpts.groups = [grp];

                    if (bedColumns.length > 9) {
                        var geneId = bedColumns[9];
                        var geneName = geneId;
                        if (bedColumns.length > 10) {
                            geneName = bedColumns[10];
                        }
                        var gg = shallowCopy(grp);
                        gg.id = geneId;
                        gg.label = geneName;
                        gg.type = 'gene';
                        featureOpts.groups.push(gg);
                    }

                    var spanList = [];
                    for (var b = 0; b < blockCount; ++b) {
                        var bmin = (blockStarts[b]|0) + start;
                        var bmax = bmin + (blockSizes[b]|0);
                        var span = new Range(bmin, bmax);
                        spanList.push(span);
                    }
                    var spans = union(spanList);
                    
                    var tsList = spans.ranges();
                    for (var s = 0; s < tsList.length; ++s) {
                        var ts = tsList[s];
                        createFeature(chromId, ts.min() + 1, ts.max(), featureOpts);
                    }

                    if (thickEnd > thickStart) {
                        var tl = intersection(spans, new Range(thickStart, thickEnd));
                        if (tl) {
                            featureOpts.type = 'translation';
                            var tlList = tl.ranges();
                            for (var s = 0; s < tlList.length; ++s) {
                                var ts = tlList[s];
                                createFeature(chromId, ts.min() + 1, ts.max(), featureOpts);
                            }
                        }
                    }
                }
            }
        }
    } else {
        dlog("Don't know what to do with " + this.bwg.type);
    }
}

//
// nasty cut/paste, should roll back in!
//

BigWigView.prototype.getFirstAdjacent = function(chrName, pos, dir, callback) {
    var chr = this.bwg.chromsToIDs[chrName];
    if (chr === undefined) {
        // Not an error because some .bwgs won't have data for all chromosomes.

        // dlog("Couldn't find chr " + chrName);
        // dlog('Chroms=' + miniJSONify(this.bwg.chromsToIDs));
        return callback([]);
    } else {
        this.getFirstAdjacentById(chr, pos, dir, callback);
    }
}

BigWigView.prototype.getFirstAdjacentById = function(chr, pos, dir, callback) {
    var thisB = this;
    if (!this.cirHeader) {
        this.bwg.data.slice(this.cirTreeOffset, 48).fetch(function(result) {
            thisB.cirHeader = result;
            var la = new Int32Array(thisB.cirHeader);
            thisB.cirBlockSize = la[1];
            thisB.getFirstAdjacentById(chr, pos, dir, callback);
        });
        return;
    }

    var blockToFetch = null;
    var bestBlockChr = -1;
    var bestBlockOffset = -1;

    var outstanding = 0;

    var beforeBWG = Date.now();

    var cirFobRecur = function(offset, level) {
        outstanding += offset.length;

        var maxCirBlockSpan = 4 +  (thisB.cirBlockSize * 32);   // Upper bound on size, based on a completely full leaf node.
        var spans;
        for (var i = 0; i < offset.length; ++i) {
            var blockSpan = new Range(offset[i], offset[i] + maxCirBlockSpan);
            spans = spans ? union(spans, blockSpan) : blockSpan;
        }
        
        var fetchRanges = spans.ranges();
        for (var r = 0; r < fetchRanges.length; ++r) {
            var fr = fetchRanges[r];
            cirFobStartFetch(offset, fr, level);
        }
    }

    var cirFobStartFetch = function(offset, fr, level, attempts) {
        var length = fr.max() - fr.min();
        thisB.bwg.data.slice(fr.min(), fr.max() - fr.min()).fetch(function(resultBuffer) {
            for (var i = 0; i < offset.length; ++i) {
                if (fr.contains(offset[i])) {
                    cirFobRecur2(resultBuffer, offset[i] - fr.min(), level);
                    --outstanding;
                    if (outstanding == 0) {
                        if (!blockToFetch) {
                            if (dir > 0 && (chr != 0 || pos > 0)) {
                                return thisB.getFirstAdjacentById(0, 0, dir, callback);
                            } else if (dir < 0 && (chr != thisB.bwg.maxID || pos < 1000000000)) {
                                return thisB.getFirstAdjacentById(thisB.bwg.maxID, 1000000000, dir, callback);
                            }
                            return callback([]);
                        }

                        thisB.fetchFeatures(function(chrx, fmin, fmax, toks) {
                            return (dir < 0 && (chrx < chr || fmax < pos)) || (dir > 0 && (chrx > chr || fmin > pos));
                        }, [blockToFetch], function(features) {
                            var bestFeature = null;
                            var bestChr = -1;
                            var bestPos = -1;
                            for (var fi = 0; fi < features.length; ++fi) {
                                var f = features[fi];
                                var chrx = f._chromId, fmin = f.min, fmax = f.max;
                                if (bestFeature == null || ((dir < 0) && (chrx > bestChr || fmax > bestPos)) || ((dir > 0) && (chrx < bestChr || fmin < bestPos))) {
                                    bestFeature = f;
                                    bestPos = (dir < 0) ? fmax : fmin;
                                    bestChr = chrx;
                                }
                            }

                            if (bestFeature != null) 
                                return callback([bestFeature]);
                            else
                                return callback([]);
                        });
                    }
                }
            }
        });
    }

    var cirFobRecur2 = function(cirBlockData, offset, level) {
        var ba = new Uint8Array(cirBlockData);
        var sa = new Int16Array(cirBlockData);
        var la = new Int32Array(cirBlockData);

        var isLeaf = ba[offset];
        var cnt = sa[offset/2 + 1];
        offset += 4;

        if (isLeaf != 0) {
            for (var i = 0; i < cnt; ++i) {
                var lo = offset/4;
                var startChrom = la[lo];
                var startBase = la[lo + 1];
                var endChrom = la[lo + 2];
                var endBase = la[lo + 3];
                var blockOffset = bwg_readOffset(ba, offset+16);
                var blockSize = bwg_readOffset(ba, offset+24);
                if ((dir < 0 && ((startChrom < chr || (startChrom == chr && startBase <= pos)))) ||
                    (dir > 0 && ((endChrom > chr || (endChrom == chr && endBase >= pos)))))
                {
                    // console.log('Got an interesting block: startBase=' + startChrom + ':' + startBase + '; endBase=' + endChrom + ':' + endBase + '; offset=' + blockOffset + '; size=' + blockSize);
                    if (/_random/.exec(thisB.bwg.idsToChroms[startChrom])) {
                        // dlog('skipping random: ' + thisB.bwg.idsToChroms[startChrom]);
                    } else if (blockToFetch == null || ((dir < 0) && (endChrom > bestBlockChr || (endChrom == bestBlockChr && endBase > bestBlockOffset)) ||
                                                 (dir > 0) && (startChrom < bestBlockChr || (startChrom == bestBlockChr && startBase < bestBlockOffset))))
                    {
                        //                        dlog('best is: startBase=' + startChrom + ':' + startBase + '; endBase=' + endChrom + ':' + endBase + '; offset=' + blockOffset + '; size=' + blockSize);
                        blockToFetch = {offset: blockOffset, size: blockSize};
                        bestBlockOffset = (dir < 0) ? endBase : startBase;
                        bestBlockChr = (dir < 0) ? endChrom : startChrom;
                    }
                }
                offset += 32;
            }
        } else {
            var bestRecur = -1;
            var bestPos = -1;
            var bestChr = -1;
            for (var i = 0; i < cnt; ++i) {
                var lo = offset/4;
                var startChrom = la[lo];
                var startBase = la[lo + 1];
                var endChrom = la[lo + 2];
                var endBase = la[lo + 3];
                var blockOffset = (la[lo + 4]<<32) | (la[lo + 5]);
                if ((dir < 0 && ((startChrom < chr || (startChrom == chr && startBase <= pos)) &&
                                 (endChrom   >= chr))) ||
                     (dir > 0 && ((endChrom > chr || (endChrom == chr && endBase >= pos)) &&
                                  (startChrom <= chr))))
                {
                    if (bestRecur < 0 || endBase > bestPos) {
                        bestRecur = blockOffset;
                        bestPos = (dir < 0) ? endBase : startBase;
                        bestChr = (dir < 0) ? endChrom : startChrom;
                    }
                }
                offset += 24;
            }
            if (bestRecur >= 0) {
                cirFobRecur([bestRecur], level + 1);
            }
        }
    };
    

    cirFobRecur([thisB.cirTreeOffset + 48], 1);
}

BigWig.prototype.readWigData = function(chrName, min, max, callback) {
    this.getUnzoomedView().readWigData(chrName, min, max, callback);
}

BigWig.prototype.getUnzoomedView = function() {
    if (!this.unzoomedView) {
        var cirLen = 4000;
        var nzl = this.zoomLevels[0];
        if (nzl) {
            cirLen = this.zoomLevels[0].dataOffset - this.unzoomedIndexOffset;
        }
        this.unzoomedView = new BigWigView(this, this.unzoomedIndexOffset, cirLen, false);
    }
    return this.unzoomedView;
}

BigWig.prototype.getZoomedView = function(z) {
    var zh = this.zoomLevels[z];
    if (!zh.view) {
        zh.view = new BigWigView(this, zh.indexOffset, /* this.zoomLevels[z + 1].dataOffset - zh.indexOffset */ 4000, true);
    }
    return zh.view;
}

function makeBwg(data, callback, name) {
    var bwg = new BigWig();
    bwg.data = data;
    bwg.name = name;
    bwg.data.slice(0, 512).salted().fetch(function(result) {
        if (!result) {
            return callback(null, "Couldn't fetch file");
        }

        var header = result;
        var ba = new Uint8Array(header);
        var sa = new Int16Array(header);
        var la = new Int32Array(header);
        var magic = ba[0] + (M1 * ba[1]) + (M2 * ba[2]) + (M3 * ba[3]);
        if (magic == BIG_WIG_MAGIC) {
            bwg.type = 'bigwig';
        } else if (magic == BIG_BED_MAGIC) {
            bwg.type = 'bigbed';
        } else if (magic == BIG_WIG_MAGIC_BE || magic == BIG_BED_MAGIC_BE) {
            callback(null, "Currently don't support big-endian BBI files");
        } else {
            callback(null, "Not a supported format, magic=0x" + magic.toString(16));
        }

        bwg.version = sa[2];             // 4
        bwg.numZoomLevels = sa[3];       // 6
        bwg.chromTreeOffset = bwg_readOffset(ba, 8);
        bwg.unzoomedDataOffset = bwg_readOffset(ba, 16);
        bwg.unzoomedIndexOffset = bwg_readOffset(ba, 24);
        bwg.fieldCount = sa[16];         // 32
        bwg.definedFieldCount = sa[17];  // 34
        bwg.asOffset = bwg_readOffset(ba, 36);
        bwg.totalSummaryOffset = bwg_readOffset(ba, 44);
        bwg.uncompressBufSize = la[13];  // 52
        bwg.extHeaderOffset = bwg_readOffset(ba, 56);

        bwg.zoomLevels = [];
        for (var zl = 0; zl < bwg.numZoomLevels; ++zl) {
            var zlReduction = la[zl*6 + 16]
            var zlData = bwg_readOffset(ba, zl*24 + 72);
            var zlIndex = bwg_readOffset(ba, zl*24 + 80);
            bwg.zoomLevels.push({reduction: zlReduction, dataOffset: zlData, indexOffset: zlIndex});
        }

        bwg.readChromTree(function() {
            bwg.getAutoSQL(function(as) {
                bwg.schema = as;
                return callback(bwg);
            });
        });
    });
}


BigWig.prototype._tsFetch = function(zoom, chr, min, max, callback) {
    var bwg = this;
    if (zoom >= this.zoomLevels.length - 1) {
        if (!this.topLevelReductionCache) {
            this.getZoomedView(this.zoomLevels.length - 1).readWigDataById(-1, 0, 300000000, function(feats) {
                bwg.topLevelReductionCache = feats;
                return bwg._tsFetch(zoom, chr, min, max, callback);
            });
        } else {
            var f = [];
            var c = this.topLevelReductionCache;
            for (var fi = 0; fi < c.length; ++fi) {
                if (c[fi]._chromId == chr) {
                    f.push(c[fi]);
                }
            }
            return callback(f);
        }
    } else {
        var view;
        if (zoom < 0) {
            view = this.getUnzoomedView();
        } else {
            view = this.getZoomedView(zoom);
        }
        return view.readWigDataById(chr, min, max, callback);
    }
}

BigWig.prototype.thresholdSearch = function(chrName, referencePoint, dir, threshold, callback) {
    dir = (dir<0) ? -1 : 1;
    var bwg = this;
    var initialChr = this.chromsToIDs[chrName];
    var candidates = [{chrOrd: 0, chr: initialChr, zoom: bwg.zoomLevels.length - 4, min: 0, max: 300000000, fromRef: true}]
    for (var i = 1; i <= this.maxID + 1; ++i) {
        var chrId = (initialChr + (dir*i)) % (this.maxID + 1);
        if (chrId < 0) 
            chrId += (this.maxID + 1);
        candidates.push({chrOrd: i, chr: chrId, zoom: bwg.zoomLevels.length - 1, min: 0, max: 300000000})
    }
       
    function fbThresholdSearchRecur() {
    	if (candidates.length == 0) {
    	    return callback(null);
    	}
    	candidates.sort(function(c1, c2) {
    	    var d = c1.zoom - c2.zoom;
    	    if (d != 0)
    		    return d;

            d = c1.chrOrd - c2.chrOrd;
            if (d != 0)
                return d;
    	    else
    		    return c1.min - c2.min * dir;
    	});

	    var candidate = candidates.splice(0, 1)[0];
        bwg._tsFetch(candidate.zoom, candidate.chr, candidate.min, candidate.max, function(feats) {
            var rp = dir > 0 ? 0 : 300000000;
            if (candidate.fromRef)
                rp = referencePoint;
            
            for (var fi = 0; fi < feats.length; ++fi) {
    	        var f = feats[fi];
                var score;
                if (f.maxScore != undefined)
                    score = f.maxScore;
                else
                    score = f.score;

                if (dir > 0) {
    	            if (score > threshold) {
        		        if (candidate.zoom < 0) {
        		            if (f.min > rp)
                                return callback(f);
        		        } else if (f.max > rp) {
        		            candidates.push({chr: candidate.chr, chrOrd: candidate.chrOrd, zoom: candidate.zoom - 2, min: f.min, max: f.max, fromRef: candidate.fromRef});
        		        }
                    }
                } else {
                    if (score > threshold) {
            		    if (candidate.zoom < 0) {
                	        if (f.max < rp)
                			    return callback(f);
                        } else if (f.min < rp) {
                            candidates.push({chr: candidate.chr, chrOrd: candidate.chrOrd, zoom: candidate.zoom - 2, min: f.min, max: f.max, fromRef: candidate.fromRef});
                        }
    	            }
                }
    	    }
            fbThresholdSearchRecur();
        });
    }
    
    fbThresholdSearchRecur();
}

BigWig.prototype.getAutoSQL = function(callback) {
    var thisB = this;
    if (!this.asOffset)
        return callback(null);


    this.data.slice(this.asOffset, 2048).fetch(function(result) {
        var ba = new Uint8Array(result);
        var s = '';
        for (var i = 0; i < ba.length; ++i) {
            if (ba[i] == 0)
                break;
            s += String.fromCharCode(ba[i]);
        }
        
        /* 
         * Quick'n'dirty attempt to parse autoSql format.
         * See: http://www.linuxjournal.com/files/linuxjournal.com/linuxjournal/articles/059/5949/5949l2.html
         */

        var header_re = /(\w+)\s+(\w+)\s+("([^"]+)")?\s+\(\s*/;
        var field_re = /([\w\[\]]+)\s+(\w+)\s*;\s*("([^"]+)")?\s*/g;

        var headerMatch = header_re.exec(s);
        if (headerMatch) {
            var as = {
                declType: headerMatch[1],
                name: headerMatch[2],
                comment: headerMatch[4],

                fields: []
            };

            s = s.substring(headerMatch[0]);
            for (var m = field_re.exec(s); m != null; m = field_re.exec(s)) {
                as.fields.push({type: m[1],
                             name: m[2],
                             comment: m[4]});
            }

            return callback(as);
        }
    });
}

BigWig.prototype.getExtraIndices = function(callback) {
    var thisB = this;
    if (this.version < 4 || this.extHeaderOffset == 0 || this.type != 'bigbed') {
        return callback(null);
    } else {
        this.data.slice(this.extHeaderOffset, 64).fetch(function(result) {
            if (!result) {
                return callback(null, "Couldn't fetch extension header");
            }

            var ba = new Uint8Array(result);
            var sa = new Int16Array(result);
            var la = new Int32Array(result);
            
            var extHeaderSize = sa[0];
            var extraIndexCount = sa[1];
            var extraIndexListOffset = bwg_readOffset(ba, 4);

            if (extraIndexCount == 0) {
                return callback(null);
            }

            // FIXME 20byte records only make sense for single-field indices.
            // Right now, these seem to be the only things around, but the format
            // is actually more general.
            thisB.data.slice(extraIndexListOffset, extraIndexCount * 20).fetch(function(eil) {
                if (!eil) {
                    return callback(null, "Couldn't fetch index info");
                }

                var ba = new Uint8Array(eil);
                var sa = new Int16Array(eil);
                var la = new Int32Array(eil);

                var indices = [];
                for (var ii = 0; ii < extraIndexCount; ++ii) {
                    var eiType = sa[ii*10];
                    var eiFieldCount = sa[ii*10 + 1];
                    var eiOffset = bwg_readOffset(ba, ii*20 + 4);
                    var eiField = sa[ii*10 + 8]
                    var index = new BBIExtraIndex(thisB, eiType, eiFieldCount, eiOffset, eiField);
                    indices.push(index);
                }
                callback(indices);
            });
        });
    }
}

function BBIExtraIndex(bbi, type, fieldCount, offset, field) {
    this.bbi = bbi;
    this.type = type;
    this.fieldCount = fieldCount;
    this.offset = offset;
    this.field = field;
}

BBIExtraIndex.prototype.lookup = function(name, callback) {
    var thisB = this;

    this.bbi.data.slice(this.offset, 32).fetch(function(bpt) {
        var ba = new Uint8Array(bpt);
        var sa = new Int16Array(bpt);
        var la = new Int32Array(bpt);
        var bptMagic = la[0];
        var blockSize = la[1];
        var keySize = la[2];
        var valSize = la[3];
        var itemCount = bwg_readOffset(ba, 16);
        var rootNodeOffset = 32;

        function bptReadNode(nodeOffset) {
            thisB.bbi.data.slice(nodeOffset, 4 + (blockSize * (keySize + valSize))).fetch(function(node) {
                var ba = new Uint8Array(node);
                var sa = new Uint16Array(node);
                var la = new Uint32Array(node);

                var nodeType = ba[0];
                var cnt = sa[1];

                var offset = 4;
                if (nodeType == 0) {
                    var lastChildOffset = null;
                    for (var n = 0; n < cnt; ++n) {
                        var key = '';
                        for (var ki = 0; ki < keySize; ++ki) {
                            var charCode = ba[offset++];
                            if (charCode != 0) {
                                key += String.fromCharCode(charCode);
                            }
                        }

                        var childOffset = bwg_readOffset(ba, offset);
                        offset += 8;
                        
                        if (name.localeCompare(key) < 0 && lastChildOffset) {
                            bptReadNode(lastChildOffset);
                            return;
                        }
                        lastChildOffset = childOffset;
                    }
                    bptReadNode(lastChildOffset);
                } else {
                    for (var n = 0; n < cnt; ++n) {
                        var key = '';
                        for (var ki = 0; ki < keySize; ++ki) {
                            var charCode = ba[offset++];
                            if (charCode != 0) {
                                key += String.fromCharCode(charCode);
                            }
                        }
                        
                        // Specific for EI case.
                        if (key == name) {
                            var start = bwg_readOffset(ba, offset);
                            var length = readInt(ba, offset + 8);

                            return thisB.bbi.getUnzoomedView().fetchFeatures(
                                function(chr, min, max, toks) {
                                    if (toks && toks.length > thisB.field - 3)
                                        return toks[thisB.field - 3] == name;
                                }, 
                                [{offset: start, size: length}], 
                                callback);
                        }
                        offset += valSize;
                    }
                    return callback([]);
                }
            });
        }

        bptReadNode(thisB.offset + rootNodeOffset);
    });
}

if (typeof(module) !== 'undefined') {
    module.exports = {
        makeBwg: makeBwg,
        BIG_BED_MAGIC: BIG_BED_MAGIC,
        BIG_WIG_MAGIC: BIG_WIG_MAGIC
    }
}

},{"./bin":3,"./das":5,"./spans":9,"./utils":10,"jszlib":11}],3:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2011
//
// bin.js general binary data support
//

"use strict";

if (typeof(require) !== 'undefined') {
    var utils = require('./utils');
    var shallowCopy = utils.shallowCopy;

    var sha1 = require('./sha1');
    var b64_sha1 = sha1.b64_sha1;
}

function BlobFetchable(b) {
    this.blob = b;
}

BlobFetchable.prototype.slice = function(start, length) {
    var b;

    if (this.blob.slice) {
        if (length) {
            b = this.blob.slice(start, start + length);
        } else {
            b = this.blob.slice(start);
        }
    } else {
        if (length) {
            b = this.blob.webkitSlice(start, start + length);
        } else {
            b = this.blob.webkitSlice(start);
        }
    }
    return new BlobFetchable(b);
}

BlobFetchable.prototype.salted = function() {return this;}

if (typeof(FileReader) !== 'undefined') {
    // console.log('defining async BlobFetchable.fetch');

    BlobFetchable.prototype.fetch = function(callback) {
        var reader = new FileReader();
        reader.onloadend = function(ev) {
            callback(bstringToBuffer(reader.result));
        };
        reader.readAsBinaryString(this.blob);
    }

} else {
    // if (console && console.log)
    //    console.log('defining sync BlobFetchable.fetch');

    BlobFetchable.prototype.fetch = function(callback) {
        var reader = new FileReaderSync();
        try {
            var res = reader.readAsArrayBuffer(this.blob);
            callback(res);
        } catch (e) {
            callback(null, e);
        }
    }
}

function URLFetchable(url, start, end, opts) {
    if (!opts) {
        if (typeof start === 'object') {
            opts = start;
            start = undefined;
        } else {
            opts = {};
        }
    }

    this.url = url;
    this.start = start || 0;
    if (end) {
        this.end = end;
    }
    this.opts = opts;
}

URLFetchable.prototype.slice = function(s, l) {
    if (s < 0) {
        throw 'Bad slice ' + s;
    }

    var ns = this.start, ne = this.end;
    if (ns && s) {
        ns = ns + s;
    } else {
        ns = s || ns;
    }
    if (l && ns) {
        ne = ns + l - 1;
    } else {
        ne = ne || l - 1;
    }
    return new URLFetchable(this.url, ns, ne, this.opts);
}

var seed=0;
var isSafari = navigator.userAgent.indexOf('Safari') >= 0 && navigator.userAgent.indexOf('Chrome') < 0 ;

URLFetchable.prototype.fetchAsText = function(callback) {
    var req = new XMLHttpRequest();
    var length;
    var url = this.url;
    if (isSafari || this.opts.salt) {
        url = saltURL(url);
        url = url + '?salt=' + b64_sha1('' + Date.now() + ',' + (++seed));
    }
    req.open('GET', url, true);

    if (this.end) {
        if (this.end - this.start > 100000000) {
            throw 'Monster fetch!';
        }
        req.setRequestHeader('Range', 'bytes=' + this.start + '-' + this.end);
        length = this.end - this.start + 1;
    }

    req.onreadystatechange = function() {
        if (req.readyState == 4) {
            if (req.status == 200 || req.status == 206) {
                return callback(req.responseText);
            } else {
                return callback(null);
            }
        }
    };
    if (this.opts.credentials) {
        req.withCredentials = true;
    }
    req.send('');
}

URLFetchable.prototype.salted = function() {
    var o = shallowCopy(this.opts);
    o.salt = true;
    return new URLFetchable(this.url, this.start, this.end, o);
}

URLFetchable.prototype.fetch = function(callback, attempt, truncatedLength) {
    var thisB = this;

    attempt = attempt || 1;
    if (attempt > 3) {
        return callback(null);
    }

    var req = new XMLHttpRequest();
    var length;
    var url = this.url;
    if (isSafari || this.opts.salt) {
        url = url + '?salt=' + b64_sha1('' + Date.now() + ',' + (++seed));
    }
    req.open('GET', url, true);
    req.overrideMimeType('text/plain; charset=x-user-defined');
    if (this.end) {
        if (this.end - this.start > 100000000) {
            throw 'Monster fetch!';
        }
        req.setRequestHeader('Range', 'bytes=' + this.start + '-' + this.end);
        length = this.end - this.start + 1;
    }
    req.responseType = 'arraybuffer';
    req.onreadystatechange = function() {
        if (req.readyState == 4) {
            if (req.status == 200 || req.status == 206) {
                if (req.response) {
                    var bl = req.response.byteLength;
                    if (length && length != bl && (!truncatedLength || bl != truncatedLength)) {
                        return thisB.fetch(callback, attempt + 1, bl);
                    } else {
                        return callback(req.response);
                    }
                } else if (req.mozResponseArrayBuffer) {
                    return callback(req.mozResponseArrayBuffer);
                } else {
                    var r = req.responseText;
                    if (length && length != r.length && (!truncatedLength || r.length != truncatedLength)) {
                        return thisB.fetch(callback, attempt + 1, r.length);
                    } else {
                        return callback(bstringToBuffer(req.responseText));
                    }
                }
            } else {
                return thisB.fetch(callback, attempt + 1);
            }
        }
    };
    if (this.opts.credentials) {
        req.withCredentials = true;
    }
    req.send('');
}

function bstringToBuffer(result) {
    if (!result) {
        return null;
    }

    var ba = new Uint8Array(result.length);
    for (var i = 0; i < ba.length; ++i) {
        ba[i] = result.charCodeAt(i);
    }
    return ba.buffer;
}

// Read from Uint8Array

(function(global) {
    var convertBuffer = new ArrayBuffer(8);
    var ba = new Uint8Array(convertBuffer);
    var fa = new Float32Array(convertBuffer);


    global.readFloat = function(buf, offset) {
        ba[0] = buf[offset];
        ba[1] = buf[offset+1];
        ba[2] = buf[offset+2];
        ba[3] = buf[offset+3];
        return fa[0];
    };
 }(this));

function readInt64(ba, offset) {
    return (ba[offset + 7] << 24) | (ba[offset + 6] << 16) | (ba[offset + 5] << 8) | (ba[offset + 4]);
}

function readInt(ba, offset) {
    return (ba[offset + 3] << 24) | (ba[offset + 2] << 16) | (ba[offset + 1] << 8) | (ba[offset]);
}

function readShort(ba, offset) {
    return (ba[offset + 1] << 8) | (ba[offset]);
}

function readByte(ba, offset) {
    return ba[offset];
}

function readIntBE(ba, offset) {
    return (ba[offset] << 24) | (ba[offset + 1] << 16) | (ba[offset + 2] << 8) | (ba[offset + 3]);
}

// Exports if we are being used as a module

if (typeof(module) !== 'undefined') {
    module.exports = {
        BlobFetchable: BlobFetchable,
        URLFetchable: URLFetchable,

        readInt: readInt,
        readIntBE: readIntBE,
        readInt64: readInt64,
        readShort: readShort,
        readByte: readByte,
        readFloat: this.readFloat
    }
}

},{"./sha1":8,"./utils":10}],4:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2010
//
// color.js
//

"use strict";

function DColour(red, green, blue, name) {
    this.red = red|0;
    this.green = green|0;
    this.blue = blue|0;
    if (name) {
        this.name = name;
    }
}

DColour.prototype.toSvgString = function() {
    if (!this.name) {
        this.name = "rgb(" + this.red + "," + this.green + "," + this.blue + ")";
    }

    return this.name;
}

function hex2(x) {
    var y = '00' + x.toString(16);
    return y.substring(y.length - 2);
}

DColour.prototype.toHexString = function() {
    return '#' + hex2(this.red) + hex2(this.green) + hex2(this.blue);
}

var palette = {
    red: new DColour(255, 0, 0, 'red'),
    green: new DColour(0, 255, 0, 'green'),
    blue: new DColour(0, 0, 255, 'blue'),
    yellow: new DColour(255, 255, 0, 'yellow'),
    white: new DColour(255, 255, 255, 'white'),
    black: new DColour(0, 0, 0, 'black'),
    gray: new DColour(180, 180, 180, 'gray'),
    grey: new DColour(180, 180, 180, 'grey')
};

var COLOR_RE = new RegExp('^#([0-9A-Fa-f]{2})([0-9A-Fa-f]{2})([0-9A-Fa-f]{2})$');
var CSS_COLOR_RE = /rgb\(([0-9]+),([0-9]+),([0-9]+)\)/

function dasColourForName(name) {
    var c = palette[name];
    if (!c) {
        var match = COLOR_RE.exec(name);
        if (match) {
            c = new DColour(('0x' + match[1])|0, ('0x' + match[2])|0, ('0x' + match[3])|0, name);
            palette[name] = c;
        } else {
    	    match = CSS_COLOR_RE.exec(name);
    	    if (match) {
        		c = new DColour(match[1]|0, match[2]|0, match[3]|0, name);
        		palette[name] = c;
	       } else {
		      console.log("couldn't handle color: " + name);
		      c = palette.black;
		      palette[name] = c;
	       }
        }
    }
    return c;
}

function makeColourSteps(steps, stops, colours) {
    var dcolours = [];
    for (var ci = 0; ci < colours.length; ++ci) {
        dcolours.push(dasColourForName(colours[ci]));
    }

    var grad = [];
  STEP_LOOP:
    for (var si = 0; si < steps; ++si) {
        var rs = (1.0 * si) / (steps-1);
        var score = stops[0] + (stops[stops.length -1] - stops[0]) * rs;
        for (var i = 0; i < stops.length - 1; ++i) {
            if (score >= stops[i] && score <= stops[i+1]) {
                var frac = (score - stops[i]) / (stops[i+1] - stops[i]);
                var ca = dcolours[i];
                var cb = dcolours[i+1];

                var fill = new DColour(
                    ((ca.red * (1.0 - frac)) + (cb.red * frac))|0,
                    ((ca.green * (1.0 - frac)) + (cb.green * frac))|0,
                    ((ca.blue * (1.0 - frac)) + (cb.blue * frac))|0
                ).toSvgString();
                grad.push(fill);

                continue STEP_LOOP;
            }
        }
        throw 'Bad step';
    }

    return grad;
}

function makeGradient(steps, color1, color2, color3) {
    if (color3) {
        return makeColourSteps(steps, [0, 0.5, 1], [color1, color2, color3]);
    } else {
        return makeColourSteps(steps, [0, 1], [color1, color2]);
    }
}

if (typeof(module) !== 'undefined') {
    module.exports = {
        makeColourSteps: makeColourSteps,
        makeGradient: makeGradient,
        dasColourForName: dasColourForName
    };
}

},{}],5:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2010
//
// das.js: queries and low-level data model.
//

"use strict";

if (typeof(require) !== 'undefined') {
    var utils = require('./utils');
    var shallowCopy = utils.shallowCopy;
    var pusho = utils.pusho;

    var color = require('./color');
    var makeColourSteps = color.makeColourSteps;
}

var dasLibErrorHandler = function(errMsg) {
    alert(errMsg);
}
var dasLibRequestQueue = new Array();



function DASSegment(name, start, end, description) {
    this.name = name;
    this.start = start;
    this.end = end;
    this.description = description;
}
DASSegment.prototype.toString = function() {
    return this.name + ':' + this.start + '..' + this.end;
};
DASSegment.prototype.isBounded = function() {
    return this.start && this.end;
}
DASSegment.prototype.toDASQuery = function() {
    var q = 'segment=' + this.name;
    if (this.start && this.end) {
        q += (':' + this.start + ',' + this.end);
    }
    return q;
}


function DASSource(a1, a2) {
    var options;
    if (typeof a1 == 'string') {
        this.uri = a1;
        options = a2 || {};
    } else {
        options = a1 || {};
    }
    for (var k in options) {
        if (typeof(options[k]) != 'function') {
            this[k] = options[k];
        }
    }


    if (!this.coords) {
        this.coords = [];
    }
    if (!this.props) {
        this.props = {};
    }

    this.dasBaseURI = this.uri;
    if (this.dasBaseURI && this.dasBaseURI.substr(this.uri.length - 1) != '/') {
        this.dasBaseURI = this.dasBaseURI + '/';
    }
}

function DASCoords() {
}

function coordsMatch(c1, c2) {
    return c1.taxon == c2.taxon && c1.auth == c2.auth && c1.version == c2.version;
}

//
// DAS 1.6 entry_points command
//

DASSource.prototype.entryPoints = function(callback) {
    var dasURI = this.dasBaseURI + 'entry_points';
    this.doCrossDomainRequest(dasURI, function(responseXML) {
            if (!responseXML) {
                return callback([]);
            }

                var entryPoints = new Array();
                
                var segs = responseXML.getElementsByTagName('SEGMENT');
                for (var i = 0; i < segs.length; ++i) {
                    var seg = segs[i];
                    var segId = seg.getAttribute('id');
                    
                    var segSize = seg.getAttribute('size');
                    var segMin, segMax;
                    if (segSize) {
                        segMin = 1; segMax = segSize|0;
                    } else {
                        segMin = seg.getAttribute('start');
                        if (segMin) {
                            segMin |= 0;
                        }
                        segMax = seg.getAttribute('stop');
                        if (segMax) {
                            segMax |= 0;
                        }
                    }
                    var segDesc = null;
                    if (seg.firstChild) {
                        segDesc = seg.firstChild.nodeValue;
                    }
                    entryPoints.push(new DASSegment(segId, segMin, segMax, segDesc));
                }          
               callback(entryPoints);
    });         
}

//
// DAS 1.6 sequence command
// Do we need an option to fall back to the dna command?
//

function DASSequence(name, start, end, alpha, seq) {
    this.name = name;
    this.start = start;
    this.end = end;
    this.alphabet = alpha;
    this.seq = seq;
}

DASSource.prototype.sequence = function(segment, callback) {
    var dasURI = this.dasBaseURI + 'sequence?' + segment.toDASQuery();
    this.doCrossDomainRequest(dasURI, function(responseXML) {
        if (!responseXML) {
            callback([]);
            return;
        } else {
                var seqs = new Array();
                
                var segs = responseXML.getElementsByTagName('SEQUENCE');
                for (var i = 0; i < segs.length; ++i) {
                    var seg = segs[i];
                    var segId = seg.getAttribute('id');
                    var segMin = seg.getAttribute('start');
                    var segMax = seg.getAttribute('stop');
                    var segAlpha = 'DNA';
                    var segSeq = null;
                    if (seg.firstChild) {
                        var rawSeq = seg.firstChild.nodeValue;
                        segSeq = '';
                        var idx = 0;
                        while (true) {
                            var space = rawSeq.indexOf('\n', idx);
                            if (space >= 0) {
                                segSeq += rawSeq.substring(idx, space);
                                idx = space + 1;
                            } else {
                                segSeq += rawSeq.substring(idx);
                                break;
                            }
                        }
                    }
                    seqs.push(new DASSequence(segId, segMin, segMax, segAlpha, segSeq));
                }
                
                callback(seqs);
        }
    });
}

//
// DAS 1.6 features command
//

function DASFeature() {
}

function DASGroup(id) {
    if (id)
        this.id = id;
}

function DASLink(desc, uri) {
    this.desc = desc;
    this.uri = uri;
}

DASSource.prototype.features = function(segment, options, callback) {
    options = options || {};
    var thisB = this;

    var dasURI;
    if (this.features_uri) {
        dasURI = this.features_uri;
    } else {
        var filters = [];

        if (segment) {
            filters.push(segment.toDASQuery());
        } else if (options.group) {
            var g = options.group;
            if (typeof g == 'string') {
                filters.push('group_id=' + g);
            } else {
                for (var gi = 0; gi < g.length; ++gi) {
                    filters.push('group_id=' + g[gi]);
                }
            }
        }

        if (options.adjacent) {
            var adj = options.adjacent;
            if (typeof adj == 'string') {
                adj = [adj];
            }
            for (var ai = 0; ai < adj.length; ++ai) {
                filters.push('adjacent=' + adj[ai]);
            }
        }

        if (options.type) {
            if (typeof options.type == 'string') {
                filters.push('type=' + options.type);
            } else {
                for (var ti = 0; ti < options.type.length; ++ti) {
                    filters.push('type=' + options.type[ti]);
                }
            }
        }
        
        if (options.maxbins) {
            filters.push('maxbins=' + options.maxbins);
        }
        
        if (filters.length > 0) {
            dasURI = this.dasBaseURI + 'features?' + filters.join(';');
        } else {
            callback([], 'No filters specified');
        }
    } 
   

    this.doCrossDomainRequest(dasURI, function(responseXML, req) {
        if (!responseXML) {
            var msg;
            if (req.status == 0) {
                msg = 'server may not support CORS';
            } else {
                msg = 'status=' + req.status;
            }
            callback([], 'Failed request: ' + msg);
            return;
        }
/*      if (req) {
            var caps = req.getResponseHeader('X-DAS-Capabilties');
            if (caps) {
                alert(caps);
            }
        } */

        var features = new Array();
        var segmentMap = {};

        var segs = responseXML.getElementsByTagName('SEGMENT');
        for (var si = 0; si < segs.length; ++si) {
            var segmentXML = segs[si];
            var segmentID = segmentXML.getAttribute('id');
            segmentMap[segmentID] = {
                min: segmentXML.getAttribute('start'),
                max: segmentXML.getAttribute('stop')
            };
            
            var featureXMLs = segmentXML.getElementsByTagName('FEATURE');
            for (var i = 0; i < featureXMLs.length; ++i) {
                var feature = featureXMLs[i];
                var dasFeature = new DASFeature();
                
                dasFeature.segment = segmentID;
                dasFeature.id = feature.getAttribute('id');
                dasFeature.label = feature.getAttribute('label');


/*
                var childNodes = feature.childNodes;
                for (var c = 0; c < childNodes.length; ++c) {
                    var cn = childNodes[c];
                    if (cn.nodeType == Node.ELEMENT_NODE) {
                        var key = cn.tagName;
                        //var val = null;
                        //if (cn.firstChild) {
                        //   val = cn.firstChild.nodeValue;
                        //}
                        dasFeature[key] = 'x';
                    }
                } */


                var spos = elementValue(feature, "START");
                var epos = elementValue(feature, "END");
                if ((spos|0) > (epos|0)) {
                    dasFeature.min = epos|0;
                    dasFeature.max = spos|0;
                } else {
                    dasFeature.min = spos|0;
                    dasFeature.max = epos|0;
                }
                {
                    var tec = feature.getElementsByTagName('TYPE');
                    if (tec.length > 0) {
                        var te = tec[0];
                        if (te.firstChild) {
                            dasFeature.type = te.firstChild.nodeValue;
                        }
                        dasFeature.typeId = te.getAttribute('id');
                        dasFeature.typeCv = te.getAttribute('cvId');
                    }
                }
                dasFeature.type = elementValue(feature, "TYPE");
                if (!dasFeature.type && dasFeature.typeId) {
                    dasFeature.type = dasFeature.typeId; // FIXME?
                }
                
                dasFeature.method = elementValue(feature, "METHOD");
                {
                    var ori = elementValue(feature, "ORIENTATION");
                    if (!ori) {
                        ori = '0';
                    }
                    dasFeature.orientation = ori;
                }
                dasFeature.score = elementValue(feature, "SCORE");
                dasFeature.links = dasLinksOf(feature);
                dasFeature.notes = dasNotesOf(feature);
                
                var groups = feature.getElementsByTagName("GROUP");
                for (var gi  = 0; gi < groups.length; ++gi) {
                    var groupXML = groups[gi];
                    var dasGroup = new DASGroup();
                    dasGroup.type = groupXML.getAttribute('type');
                    dasGroup.id = groupXML.getAttribute('id');
                    dasGroup.links = dasLinksOf(groupXML);
                    dasGroup.notes = dasNotesOf(groupXML);
                    if (!dasFeature.groups) {
                        dasFeature.groups = new Array(dasGroup);
                    } else {
                        dasFeature.groups.push(dasGroup);
                    }
                }

                // Magic notes.  Check with TAD before changing this.
                if (dasFeature.notes) {
                    for (var ni = 0; ni < dasFeature.notes.length; ++ni) {
                        var n = dasFeature.notes[ni];
                        if (n.indexOf('Genename=') == 0) {
                            var gg = new DASGroup();
                            gg.type='gene';
                            gg.id = n.substring(9);
                            if (!dasFeature.groups) {
                                dasFeature.groups = new Array(gg);
                            } else {
                                dasFeature.groups.push(gg);
                            }
                        }
                    }
                }
                
                {
                    var pec = feature.getElementsByTagName('PART');
                    if (pec.length > 0) {
                        var parts = [];
                        for (var pi = 0; pi < pec.length; ++pi) {
                            parts.push(pec[pi].getAttribute('id'));
                        }
                        dasFeature.parts = parts;
                    }
                }
                {
                    var pec = feature.getElementsByTagName('PARENT');
                    if (pec.length > 0) {
                        var parents = [];
                        for (var pi = 0; pi < pec.length; ++pi) {
                            parents.push(pec[pi].getAttribute('id'));
                        }
                        dasFeature.parents = parents;
                    }
                }
                
                features.push(dasFeature);
            }
        }
                
        callback(features, undefined, segmentMap);
    },
    function (err) {
        callback([], err);
    });
}

function DASAlignment(type) {
    this.type = type;
    this.objects = {};
    this.blocks = [];
}

DASSource.prototype.alignments = function(segment, options, callback) {
    var dasURI = this.dasBaseURI + 'alignment?query=' + segment;
    this.doCrossDomainRequest(dasURI, function(responseXML) {
        if (!responseXML) {
            callback([], 'Failed request ' + dasURI);
            return;
        }

        var alignments = [];
        var aliXMLs = responseXML.getElementsByTagName('alignment');
        for (var ai = 0; ai < aliXMLs.length; ++ai) {
            var aliXML = aliXMLs[ai];
            var ali = new DASAlignment(aliXML.getAttribute('alignType'));
            var objXMLs = aliXML.getElementsByTagName('alignObject');
            for (var oi = 0; oi < objXMLs.length; ++oi) {
                var objXML = objXMLs[oi];
                var obj = {
                    id:          objXML.getAttribute('intObjectId'),
                    accession:   objXML.getAttribute('dbAccessionId'),
                    version:     objXML.getAttribute('objectVersion'),
                    dbSource:    objXML.getAttribute('dbSource'),
                    dbVersion:   objXML.getAttribute('dbVersion')
                };
                ali.objects[obj.id] = obj;
            }
            
            var blockXMLs = aliXML.getElementsByTagName('block');
            for (var bi = 0; bi < blockXMLs.length; ++bi) {
                var blockXML = blockXMLs[bi];
                var block = {
                    order:      blockXML.getAttribute('blockOrder'),
                    segments:   []
                };
                var segXMLs = blockXML.getElementsByTagName('segment');
                for (var si = 0; si < segXMLs.length; ++si) {
                    var segXML = segXMLs[si];
                    var seg = {
                        object:      segXML.getAttribute('intObjectId'),
                        min:         segXML.getAttribute('start'),
                        max:         segXML.getAttribute('end'),
                        strand:      segXML.getAttribute('strand'),
                        cigar:       elementValue(segXML, 'cigar')
                    };
                    block.segments.push(seg);
                }
                ali.blocks.push(block);
            }       
                    
            alignments.push(ali);
        }
        callback(alignments);
    });
}


function DASStylesheet() {
/*
    this.highZoomStyles = new Object();
    this.mediumZoomStyles = new Object();
    this.lowZoomStyles = new Object();
*/

    this.styles = [];
}

DASStylesheet.prototype.pushStyle = function(filters, zoom, style) {
    /*

    if (!zoom) {
        this.highZoomStyles[type] = style;
        this.mediumZoomStyles[type] = style;
        this.lowZoomStyles[type] = style;
    } else if (zoom == 'high') {
        this.highZoomStyles[type] = style;
    } else if (zoom == 'medium') {
        this.mediumZoomStyles[type] = style;
    } else if (zoom == 'low') {
        this.lowZoomStyles[type] = style;
    }

    */

    if (!filters) {
        filters = {type: 'default'};
    }
    var styleHolder = shallowCopy(filters);
    if (zoom) {
        styleHolder.zoom = zoom;
    }
    styleHolder.style = style;
    this.styles.push(styleHolder);
}

function DASStyle() {
}

function parseGradient(grad) {
    var steps = grad.getAttribute('steps');
    if (steps) {
        steps = steps|0;
    } else {
        steps = 50;
    }


    var stops = [];
    var colors = [];
    var se = grad.getElementsByTagName('STOP');
    for (var si = 0; si < se.length; ++si) {
        var stop = se[si];
        stops.push(1.0 * stop.getAttribute('score'));
        colors.push(stop.firstChild.nodeValue);
    }

    return makeColourSteps(steps, stops, colors);
}

DASSource.prototype.stylesheet = function(successCB, failureCB) {
    var dasURI, creds = this.credentials;
    if (this.stylesheet_uri) {
        dasURI = this.stylesheet_uri;
        creds = false;
    } else {
        dasURI = this.dasBaseURI + 'stylesheet';
    }

    doCrossDomainRequest(dasURI, function(responseXML) {
        if (!responseXML) {
            if (failureCB) {
                failureCB();
            } 
            return;
        }
        var stylesheet = new DASStylesheet();
        var typeXMLs = responseXML.getElementsByTagName('TYPE');
        for (var i = 0; i < typeXMLs.length; ++i) {
            var typeStyle = typeXMLs[i];
            
            var filter = {};
            filter.type = typeStyle.getAttribute('id'); // Am I right in thinking that this makes DASSTYLE XML invalid?  Ugh.
            filter.label = typeStyle.getAttribute('label');
            filter.method = typeStyle.getAttribute('method');
            var glyphXMLs = typeStyle.getElementsByTagName('GLYPH');
            for (var gi = 0; gi < glyphXMLs.length; ++gi) {
                var glyphXML = glyphXMLs[gi];
                var zoom = glyphXML.getAttribute('zoom');
                var glyph = childElementOf(glyphXML);
                var style = new DASStyle();
                style.glyph = glyph.localName;
                var child = glyph.firstChild;
        
                while (child) {
                    if (child.nodeType == Node.ELEMENT_NODE) {
                        // alert(child.localName);
                        if (child.localName == 'BGGRAD') {
                            style[child.localName] = parseGradient(child);
                        } else {      
                            style[child.localName] = child.firstChild.nodeValue;
                        }
                    }
                    child = child.nextSibling;
                }
                stylesheet.pushStyle(filter, zoom, style);
            }
        }
        successCB(stylesheet);
    }, creds);
}

//
// sources command
// 

function DASRegistry(uri, opts)
{
    opts = opts || {};
    this.uri = uri;
    this.opts = opts;   
}

DASRegistry.prototype.sources = function(callback, failure, opts)
{
    if (!opts) {
        opts = {};
    }

    var filters = [];
    if (opts.taxon) {
        filters.push('organism=' + opts.taxon);
    }
    if (opts.auth) {
        filters.push('authority=' + opts.auth);
    }
    if (opts.version) {
        filters.push('version=' + opts.version);
    }
    var quri = this.uri;
    if (filters.length > 0) {
        quri = quri + '?' + filters.join('&');   // '&' as a separator to hack around dasregistry.org bug.
    }

    doCrossDomainRequest(quri, function(responseXML) {
        if (!responseXML && failure) {
            failure();
            return;
        }

        var sources = [];       
        var sourceXMLs = responseXML.getElementsByTagName('SOURCE');
        for (var si = 0; si < sourceXMLs.length; ++si) {
            var sourceXML = sourceXMLs[si];
            var versionXMLs = sourceXML.getElementsByTagName('VERSION');
            if (versionXMLs.length < 1) {
                continue;
            }
            var versionXML = versionXMLs[0];

            var coordXMLs = versionXML.getElementsByTagName('COORDINATES');
            var coords = [];
            for (var ci = 0; ci < coordXMLs.length; ++ci) {
                var coordXML = coordXMLs[ci];
                var coord = new DASCoords();
                coord.auth = coordXML.getAttribute('authority');
                coord.taxon = coordXML.getAttribute('taxid');
                coord.version = coordXML.getAttribute('version');
                coords.push(coord);
            }
            
            var caps = [];
            var capXMLs = versionXML.getElementsByTagName('CAPABILITY');
            var uri;
            for (var ci = 0; ci < capXMLs.length; ++ci) {
                var capXML = capXMLs[ci];
                
                caps.push(capXML.getAttribute('type'));

                if (capXML.getAttribute('type') == 'das1:features') {
                    var fep = capXML.getAttribute('query_uri');
                    uri = fep.substring(0, fep.length - ('features'.length));
                }
            }

            var props = {};
            var propXMLs = versionXML.getElementsByTagName('PROP');
            for (var pi = 0; pi < propXMLs.length; ++pi) {
                pusho(props, propXMLs[pi].getAttribute('name'), propXMLs[pi].getAttribute('value'));
            }
            
            if (uri) {
                var source = new DASSource(uri, {
                    source_uri: sourceXML.getAttribute('uri'),
                    name:  sourceXML.getAttribute('title'),
                    desc:  sourceXML.getAttribute('description'),
                    coords: coords,
                    props: props,
                    capabilities: caps
                });
                sources.push(source);
            }
        }
        
        callback(sources);
    });
}


//
// Utility functions
//

function elementValue(element, tag)
{
    var children = element.getElementsByTagName(tag);
    if (children.length > 0 && children[0].firstChild) {
        var c = children[0];
        if (c.childNodes.length == 1) {
            return c.firstChild.nodeValue;
        } else {
            var s = '';
            for (var ni = 0; ni < c.childNodes.length; ++ni) {
                s += c.childNodes[ni].nodeValue;
            }
            return s;
        }

    } else {
        return null;
    }
}

function childElementOf(element)
{
    if (element.hasChildNodes()) {
        var child = element.firstChild;
        do {
            if (child.nodeType == Node.ELEMENT_NODE) {
                return child;
            } 
            child = child.nextSibling;
        } while (child != null);
    }
    return null;
}


function dasLinksOf(element)
{
    var links = new Array();
    var maybeLinkChilden = element.getElementsByTagName('LINK');
    for (var ci = 0; ci < maybeLinkChilden.length; ++ci) {
        var linkXML = maybeLinkChilden[ci];
        if (linkXML.parentNode == element) {
            links.push(new DASLink(linkXML.firstChild ? linkXML.firstChild.nodeValue : 'Unknown', linkXML.getAttribute('href')));
        }
    }
    
    return links;
}

function dasNotesOf(element)
{
    var notes = [];
    var maybeNotes = element.getElementsByTagName('NOTE');
    for (var ni = 0; ni < maybeNotes.length; ++ni) {
        if (maybeNotes[ni].firstChild) {
            notes.push(maybeNotes[ni].firstChild.nodeValue);
        }
    }
    return notes;
}

function doCrossDomainRequest(url, handler, credentials, custAuth) {
    // TODO: explicit error handlers?

    if (window.XDomainRequest) {
        var req = new XDomainRequest();
        req.onload = function() {
            var dom = new ActiveXObject("Microsoft.XMLDOM");
            dom.async = false;
            dom.loadXML(req.responseText);
            handler(dom);
        }
        req.open("get", url);
        req.send('');
    } else {
        var reqStart = Date.now();
        var req = new XMLHttpRequest();

        req.onreadystatechange = function() {
            if (req.readyState == 4) {
              if (req.status >= 200 || req.status == 0) {
                  handler(req.responseXML, req);
              }
            }
        };
        req.open("get", url, true);
        if (credentials) {
            req.withCredentials = true;
        }
        if (custAuth) {
            req.setRequestHeader('X-DAS-Authorisation', custAuth);
        }
        req.setRequestHeader('Accept', 'application/xml,*/*');
        req.send('');
    }
}

DASSource.prototype.doCrossDomainRequest = function(url, handler, errHandler) {
    var custAuth;
    if (this.xUser) {
        custAuth = 'Basic ' + btoa(this.xUser + ':' + this.xPass);
    }

    try {
        return doCrossDomainRequest(url, handler, this.credentials, custAuth);
    } catch (err) {
        if (errHandler) {
            errHandler(err);
        } else {
            throw err;
        }
    }
}

function isDasBooleanTrue(s) {
    s = ('' + s).toLowerCase();
    return s==='yes' || s==='true';
}

function isDasBooleanNotFalse(s) {
    if (!s)
        return false;

    s = ('' + s).toLowerCase();
    return s!=='no' || s!=='false';
}

function copyStylesheet(ss) {
    var nss = shallowCopy(ss);
    nss.styles = [];
    for (var si = 0; si < ss.styles.length; ++si) {
        var sh = nss.styles[si] = shallowCopy(ss.styles[si]);
        sh._methodRE = sh._labelRE = sh._typeRE = undefined;
        sh.style = shallowCopy(sh.style);
        sh.style.id = undefined;
    }
    return nss;
}

if (typeof(module) !== 'undefined') {
    module.exports = {
        DASGroup: DASGroup,
        DASFeature: DASFeature,
        DASStylesheet: DASStylesheet,
        DASStyle: DASStyle,
        DASSource: DASSource,
        DASSegment: DASSegment,
        DASRegistry: DASRegistry,
        DASSequence: DASSequence,

        isDasBooleanTrue: isDasBooleanTrue,
        isDasBooleanNotFalse: isDasBooleanNotFalse,
        copyStylesheet: copyStylesheet
    };
}
},{"./color":4,"./utils":10}],6:[function(require,module,exports){
(function (global){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2014
//
// fetchworker.js
//

"use strict";

var bin = require('./bin');
var bam = require('./bam');
var bigwig = require('./bigwig');

var connections = {};

var idSeed = 0;

global.newID = function() {
    return 'cn' + (++idSeed);
}

postMessage({tag: 'init'});

self.onmessage = function(event) {
    var d = event.data;
    var command = event.data.command;
    var tag = event.data.tag;

    if (command === 'connectBAM') {
        var id = newID();

        var bamF, baiF;
        if (d.blob) {
            bamF = new bin.BlobFetchable(d.blob);
            baiF = new bin.BlobFetchable(d.indexBlob);
        } else {
            bamF = new bin.URLFetchable(d.uri);
            baiF = new bin.URLFetchable(d.indexUri);
        }

        bam.makeBam(bamF, baiF, function(bamObj, err) {
            if (bamObj) {
                connections[id] = new BAMWorkerFetcher(bamObj);
                postMessage({tag: tag, result: id});
            } else {
                postMessage({tag: tag, error: err || "Couldn't fetch BAM"});
            }
        });
    } else if (command === 'connectBBI') {
        var id = newID();
        var bbi;
        if (d.blob) {
            bbi = new bin.BlobFetchable(d.blob);
        } else {
            bbi = new bin.URLFetchable(d.uri);
        }

        bigwig.makeBwg(bbi, function(bwg, err) {
            if (bwg) {
                connections[id] = new BBIWorkerFetcher(bwg);
                postMessage({tag: tag, result: id});
            } else {
                postMessage({tag: tag, error: err || "Couldn't fetch BBI"});
            }
        }, d.uri);
    } else if (command === 'fetch') {
        var con = connections[event.data.connection];
        if (!con) {
            return postMessage({tag: tag, error: 'No such connection: ' + event.data.connection});
        }

        con.fetch(d.tag, d.chr, d.min, d.max, d.zoom, d.opts);
    } else if (command === 'leap') {
        var con = connections[event.data.connection];
        if (!con) {
            return postMessage({tag: tag, error: 'No such connection: ' + event.data.connection});
        }

        con.leap(d.tag, d.chr, d.pos, d.dir);
    } else if (command === 'quantLeap') {
        var con = connections[event.data.connection];
        if (!con) {
            return postMessage({tag: tag, error: 'No such connection: ' + event.data.connection});
        }

        con.quantLeap(d.tag, d.chr, d.pos, d.dir, d.threshold, d.under);
    } else if (command === 'meta') {
        var con = connections[event.data.connection];
        if (!con) {
            return postMessage({tag: tag, error: 'No such connection: ' + event.data.connection});
        }

        con.meta(d.tag);
    } else if (command === 'search') {
        var con = connections[event.data.connection];
        if (!con) {
            return postMessage({tag: tag, error: 'No such connection: ' + event.data.connection});
        }

        con.search(d.tag, d.query, d.index);
    } else if (command === 'date') {
        return postMessage({tag: tag, result: Date.now()|0});
    } else {
        postMessage({tag: tag, error: 'Bad command ' + command});
    }
}

function BAMWorkerFetcher(bam) {
    this.bam = bam;
}

BAMWorkerFetcher.prototype.fetch = function(tag, chr, min, max, zoom, opts) {
    opts = opts || {};
    this.bam.fetch(chr, min, max, function(records, err) {
        if (records) {
            postMessage({tag: tag, result: records, time: Date.now()|0});
        } else {
            postMessage({tag: tag, error: err});
        }
    }, opts);
}

function BBIWorkerFetcher(bbi) {
    this.bbi = bbi;
}

BBIWorkerFetcher.prototype.fetch = function(tag, chr, min, max, zoom) {
    if (typeof(zoom) !== 'number')
        zoom = -1;

    var data;
    if (zoom < 0) {
        data = this.bbi.getUnzoomedView();
    } else {
        data = this.bbi.getZoomedView(zoom);
    }

    data.readWigData(chr, min, max, function(features) {
        postMessage({tag: tag, result: features});
    });
}

BBIWorkerFetcher.prototype.meta = function(tag) {
    var scales = [1];
    for (var z = 0; z < this.bbi.zoomLevels.length; ++z) {
        scales.push(this.bbi.zoomLevels[z].reduction);
    }

    var thisB = this;
    var meta = {type: this.bbi.type,
                zoomLevels: scales,
                fieldCount: this.bbi.fieldCount,
                definedFieldCount: this.bbi.definedFieldCount,
                schema: this.bbi.schema};
    if (this.bbi.type === 'bigbed') {
        this.bbi.getExtraIndices(function(ei) {
            if (ei) {
                thisB.extraIndices = ei;
                meta.extraIndices = ei.map(function(i) {return i.field});
            }
            postMessage({tag: tag, result: meta});
        });
    } else {
        postMessage({tag: tag, result: meta});
    }
}

BBIWorkerFetcher.prototype.leap = function(tag, chr, pos, dir) {
    this.bbi.getUnzoomedView().getFirstAdjacent(chr, pos, dir, function(result, err) {
        postMessage({tag: tag, result: result, error: err});
    });
}

BBIWorkerFetcher.prototype.quantLeap = function(tag, chr, pos, dir, threshold, under) {
    this.bbi.thresholdSearch(chr, pos, dir, threshold, function(result, err) {
        postMessage({tag: tag, result: result, error: err});
    });
}

BBIWorkerFetcher.prototype.search = function(tag, query, index) {
    var is = this.extraIndices[0];
    is.lookup(query, function(result, err) {
        postMessage({tag: tag, result: result, error: err});
    });
}

}).call(this,typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
},{"./bam":1,"./bigwig":2,"./bin":3}],7:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2011
//
// lh3utils.js: common support for lh3's file formats
//

if (typeof(require) !== 'undefined') {
    var jszlib = require('jszlib');
    var jszlib_inflate_buffer = jszlib.inflateBuffer;
    var arrayCopy = jszlib.arrayCopy;
}

function Vob(b, o) {
    this.block = b;
    this.offset = o;
}

Vob.prototype.toString = function() {
    return '' + this.block + ':' + this.offset;
}

function readVob(ba, offset) {
    var block = ((ba[offset+6] & 0xff) * 0x100000000) + ((ba[offset+5] & 0xff) * 0x1000000) + ((ba[offset+4] & 0xff) * 0x10000) + ((ba[offset+3] & 0xff) * 0x100) + ((ba[offset+2] & 0xff));
    var bint = (ba[offset+1] << 8) | (ba[offset]);
    if (block == 0 && bint == 0) {
        return null;  // Should only happen in the linear index?
    } else {
        return new Vob(block, bint);
    }
}

function unbgzf(data, lim) {
    lim = Math.min(lim || 1, data.byteLength - 50);
    var oBlockList = [];
    var ptr = [0];
    var totalSize = 0;

    while (ptr[0] < lim) {
        var ba = new Uint8Array(data, ptr[0], 12); // FIXME is this enough for all credible BGZF block headers?
        var xlen = (ba[11] << 8) | (ba[10]);
        // dlog('xlen[' + (ptr[0]) +']=' + xlen);
        var unc = jszlib_inflate_buffer(data, 12 + xlen + ptr[0], Math.min(65536, data.byteLength - 12 - xlen - ptr[0]), ptr);
        ptr[0] += 8;
        totalSize += unc.byteLength;
        oBlockList.push(unc);
    }

    if (oBlockList.length == 1) {
        return oBlockList[0];
    } else {
        var out = new Uint8Array(totalSize);
        var cursor = 0;
        for (var i = 0; i < oBlockList.length; ++i) {
            var b = new Uint8Array(oBlockList[i]);
            arrayCopy(b, 0, out, cursor, b.length);
            cursor += b.length;
        }
        return out.buffer;
    }
}

function Chunk(minv, maxv) {
    this.minv = minv; this.maxv = maxv;
}


//
// Binning (transliterated from SAM1.3 spec)
//

/* calculate bin given an alignment covering [beg,end) (zero-based, half-close-half-open) */
function reg2bin(beg, end)
{
    --end;
    if (beg>>14 == end>>14) return ((1<<15)-1)/7 + (beg>>14);
    if (beg>>17 == end>>17) return ((1<<12)-1)/7 + (beg>>17);
    if (beg>>20 == end>>20) return ((1<<9)-1)/7 + (beg>>20);
    if (beg>>23 == end>>23) return ((1<<6)-1)/7 + (beg>>23);
    if (beg>>26 == end>>26) return ((1<<3)-1)/7 + (beg>>26);
    return 0;
}

/* calculate the list of bins that may overlap with region [beg,end) (zero-based) */
var MAX_BIN = (((1<<18)-1)/7);
function reg2bins(beg, end) 
{
    var i = 0, k, list = [];
    --end;
    list.push(0);
    for (k = 1 + (beg>>26); k <= 1 + (end>>26); ++k) list.push(k);
    for (k = 9 + (beg>>23); k <= 9 + (end>>23); ++k) list.push(k);
    for (k = 73 + (beg>>20); k <= 73 + (end>>20); ++k) list.push(k);
    for (k = 585 + (beg>>17); k <= 585 + (end>>17); ++k) list.push(k);
    for (k = 4681 + (beg>>14); k <= 4681 + (end>>14); ++k) list.push(k);
    return list;
}

if (typeof(module) !== 'undefined') {
    module.exports = {
        unbgzf: unbgzf,
        readVob: readVob,
        reg2bin: reg2bin,
        reg2bins: reg2bins,
        Chunk: Chunk
    };
}
},{"jszlib":11}],8:[function(require,module,exports){
/*
 * A JavaScript implementation of the Secure Hash Algorithm, SHA-1, as defined
 * in FIPS 180-1
 * Version 2.2 Copyright Paul Johnston 2000 - 2009.
 * Other contributors: Greg Holt, Andrew Kepert, Ydnar, Lostinet
 * Distributed under the BSD License
 * See http://pajhome.org.uk/crypt/md5 for details.
 */

 "use strict";

/*
 * Configurable variables. You may need to tweak these to be compatible with
 * the server-side, but the defaults work in most cases.
 */
var hexcase = 0;  /* hex output format. 0 - lowercase; 1 - uppercase        */
var b64pad  = ""; /* base-64 pad character. "=" for strict RFC compliance   */

/*
 * These are the functions you'll usually want to call
 * They take string arguments and return either hex or base-64 encoded strings
 */
function hex_sha1(s)    { return rstr2hex(rstr_sha1(str2rstr_utf8(s))); }
function b64_sha1(s)    { return rstr2b64(rstr_sha1(str2rstr_utf8(s))); }
function any_sha1(s, e) { return rstr2any(rstr_sha1(str2rstr_utf8(s)), e); }
function hex_hmac_sha1(k, d)
  { return rstr2hex(rstr_hmac_sha1(str2rstr_utf8(k), str2rstr_utf8(d))); }
function b64_hmac_sha1(k, d)
  { return rstr2b64(rstr_hmac_sha1(str2rstr_utf8(k), str2rstr_utf8(d))); }
function any_hmac_sha1(k, d, e)
  { return rstr2any(rstr_hmac_sha1(str2rstr_utf8(k), str2rstr_utf8(d)), e); }

/*
 * Perform a simple self-test to see if the VM is working
 */
function sha1_vm_test()
{
  return hex_sha1("abc").toLowerCase() == "a9993e364706816aba3e25717850c26c9cd0d89d";
}

/*
 * Calculate the SHA1 of a raw string
 */
function rstr_sha1(s)
{
  return binb2rstr(binb_sha1(rstr2binb(s), s.length * 8));
}

/*
 * Calculate the HMAC-SHA1 of a key and some data (raw strings)
 */
function rstr_hmac_sha1(key, data)
{
  var bkey = rstr2binb(key);
  if(bkey.length > 16) bkey = binb_sha1(bkey, key.length * 8);

  var ipad = Array(16), opad = Array(16);
  for(var i = 0; i < 16; i++)
  {
    ipad[i] = bkey[i] ^ 0x36363636;
    opad[i] = bkey[i] ^ 0x5C5C5C5C;
  }

  var hash = binb_sha1(ipad.concat(rstr2binb(data)), 512 + data.length * 8);
  return binb2rstr(binb_sha1(opad.concat(hash), 512 + 160));
}

/*
 * Convert a raw string to a hex string
 */
function rstr2hex(input)
{
  // try { hexcase } catch(e) { hexcase=0; }
  var hex_tab = hexcase ? "0123456789ABCDEF" : "0123456789abcdef";
  var output = "";
  var x;
  for(var i = 0; i < input.length; i++)
  {
    x = input.charCodeAt(i);
    output += hex_tab.charAt((x >>> 4) & 0x0F)
           +  hex_tab.charAt( x        & 0x0F);
  }
  return output;
}

/*
 * Convert a raw string to a base-64 string
 */
function rstr2b64(input)
{
  // try { b64pad } catch(e) { b64pad=''; }
  var tab = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
  var output = "";
  var len = input.length;
  for(var i = 0; i < len; i += 3)
  {
    var triplet = (input.charCodeAt(i) << 16)
                | (i + 1 < len ? input.charCodeAt(i+1) << 8 : 0)
                | (i + 2 < len ? input.charCodeAt(i+2)      : 0);
    for(var j = 0; j < 4; j++)
    {
      if(i * 8 + j * 6 > input.length * 8) output += b64pad;
      else output += tab.charAt((triplet >>> 6*(3-j)) & 0x3F);
    }
  }
  return output;
}

/*
 * Convert a raw string to an arbitrary string encoding
 */
function rstr2any(input, encoding)
{
  var divisor = encoding.length;
  var remainders = Array();
  var i, q, x, quotient;

  /* Convert to an array of 16-bit big-endian values, forming the dividend */
  var dividend = Array(Math.ceil(input.length / 2));
  for(i = 0; i < dividend.length; i++)
  {
    dividend[i] = (input.charCodeAt(i * 2) << 8) | input.charCodeAt(i * 2 + 1);
  }

  /*
   * Repeatedly perform a long division. The binary array forms the dividend,
   * the length of the encoding is the divisor. Once computed, the quotient
   * forms the dividend for the next step. We stop when the dividend is zero.
   * All remainders are stored for later use.
   */
  while(dividend.length > 0)
  {
    quotient = Array();
    x = 0;
    for(i = 0; i < dividend.length; i++)
    {
      x = (x << 16) + dividend[i];
      q = Math.floor(x / divisor);
      x -= q * divisor;
      if(quotient.length > 0 || q > 0)
        quotient[quotient.length] = q;
    }
    remainders[remainders.length] = x;
    dividend = quotient;
  }

  /* Convert the remainders to the output string */
  var output = "";
  for(i = remainders.length - 1; i >= 0; i--)
    output += encoding.charAt(remainders[i]);

  /* Append leading zero equivalents */
  var full_length = Math.ceil(input.length * 8 /
                                    (Math.log(encoding.length) / Math.log(2)))
  for(i = output.length; i < full_length; i++)
    output = encoding[0] + output;

  return output;
}

/*
 * Encode a string as utf-8.
 * For efficiency, this assumes the input is valid utf-16.
 */
function str2rstr_utf8(input)
{
  var output = "";
  var i = -1;
  var x, y;

  while(++i < input.length)
  {
    /* Decode utf-16 surrogate pairs */
    x = input.charCodeAt(i);
    y = i + 1 < input.length ? input.charCodeAt(i + 1) : 0;
    if(0xD800 <= x && x <= 0xDBFF && 0xDC00 <= y && y <= 0xDFFF)
    {
      x = 0x10000 + ((x & 0x03FF) << 10) + (y & 0x03FF);
      i++;
    }

    /* Encode output as utf-8 */
    if(x <= 0x7F)
      output += String.fromCharCode(x);
    else if(x <= 0x7FF)
      output += String.fromCharCode(0xC0 | ((x >>> 6 ) & 0x1F),
                                    0x80 | ( x         & 0x3F));
    else if(x <= 0xFFFF)
      output += String.fromCharCode(0xE0 | ((x >>> 12) & 0x0F),
                                    0x80 | ((x >>> 6 ) & 0x3F),
                                    0x80 | ( x         & 0x3F));
    else if(x <= 0x1FFFFF)
      output += String.fromCharCode(0xF0 | ((x >>> 18) & 0x07),
                                    0x80 | ((x >>> 12) & 0x3F),
                                    0x80 | ((x >>> 6 ) & 0x3F),
                                    0x80 | ( x         & 0x3F));
  }
  return output;
}

/*
 * Encode a string as utf-16
 */
function str2rstr_utf16le(input)
{
  var output = "";
  for(var i = 0; i < input.length; i++)
    output += String.fromCharCode( input.charCodeAt(i)        & 0xFF,
                                  (input.charCodeAt(i) >>> 8) & 0xFF);
  return output;
}

function str2rstr_utf16be(input)
{
  var output = "";
  for(var i = 0; i < input.length; i++)
    output += String.fromCharCode((input.charCodeAt(i) >>> 8) & 0xFF,
                                   input.charCodeAt(i)        & 0xFF);
  return output;
}

/*
 * Convert a raw string to an array of big-endian words
 * Characters >255 have their high-byte silently ignored.
 */
function rstr2binb(input)
{
  var output = Array(input.length >> 2);
  for(var i = 0; i < output.length; i++)
    output[i] = 0;
  for(var i = 0; i < input.length * 8; i += 8)
    output[i>>5] |= (input.charCodeAt(i / 8) & 0xFF) << (24 - i % 32);
  return output;
}

/*
 * Convert an array of big-endian words to a string
 */
function binb2rstr(input)
{
  var output = "";
  for(var i = 0; i < input.length * 32; i += 8)
    output += String.fromCharCode((input[i>>5] >>> (24 - i % 32)) & 0xFF);
  return output;
}

/*
 * Calculate the SHA-1 of an array of big-endian words, and a bit length
 */
function binb_sha1(x, len)
{
  /* append padding */
  x[len >> 5] |= 0x80 << (24 - len % 32);
  x[((len + 64 >> 9) << 4) + 15] = len;

  var w = Array(80);
  var a =  1732584193;
  var b = -271733879;
  var c = -1732584194;
  var d =  271733878;
  var e = -1009589776;

  for(var i = 0; i < x.length; i += 16)
  {
    var olda = a;
    var oldb = b;
    var oldc = c;
    var oldd = d;
    var olde = e;

    for(var j = 0; j < 80; j++)
    {
      if(j < 16) w[j] = x[i + j];
      else w[j] = bit_rol(w[j-3] ^ w[j-8] ^ w[j-14] ^ w[j-16], 1);
      var t = safe_add(safe_add(bit_rol(a, 5), sha1_ft(j, b, c, d)),
                       safe_add(safe_add(e, w[j]), sha1_kt(j)));
      e = d;
      d = c;
      c = bit_rol(b, 30);
      b = a;
      a = t;
    }

    a = safe_add(a, olda);
    b = safe_add(b, oldb);
    c = safe_add(c, oldc);
    d = safe_add(d, oldd);
    e = safe_add(e, olde);
  }
  return Array(a, b, c, d, e);

}

/*
 * Perform the appropriate triplet combination function for the current
 * iteration
 */
function sha1_ft(t, b, c, d)
{
  if(t < 20) return (b & c) | ((~b) & d);
  if(t < 40) return b ^ c ^ d;
  if(t < 60) return (b & c) | (b & d) | (c & d);
  return b ^ c ^ d;
}

/*
 * Determine the appropriate additive constant for the current iteration
 */
function sha1_kt(t)
{
  return (t < 20) ?  1518500249 : (t < 40) ?  1859775393 :
         (t < 60) ? -1894007588 : -899497514;
}

/*
 * Add integers, wrapping at 2^32. This uses 16-bit operations internally
 * to work around bugs in some JS interpreters.
 */
function safe_add(x, y)
{
  var lsw = (x & 0xFFFF) + (y & 0xFFFF);
  var msw = (x >> 16) + (y >> 16) + (lsw >> 16);
  return (msw << 16) | (lsw & 0xFFFF);
}

/*
 * Bitwise rotate a 32-bit number to the left.
 */
function bit_rol(num, cnt)
{
  return (num << cnt) | (num >>> (32 - cnt));
}

if (typeof(module) !== 'undefined') {
  module.exports = {
    b64_sha1: b64_sha1,
    hex_sha1: hex_sha1
  }
}

},{}],9:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2010
//
// spans.js: JavaScript Intset/Location port.
//

"use strict";


function Range(min, max)
{
    if (typeof(min) != 'number' || typeof(max) != 'number')
        throw 'Bad range ' + min + ',' + max;
    this._min = min;
    this._max = max;
}

Range.prototype.min = function() {
    return this._min;
}

Range.prototype.max = function() {
    return this._max;
}

Range.prototype.contains = function(pos) {
    return pos >= this._min && pos <= this._max;
}

Range.prototype.isContiguous = function() {
    return true;
}

Range.prototype.ranges = function() {
    return [this];
}

Range.prototype._pushRanges = function(ranges) {
    ranges.push(this);
}

Range.prototype.toString = function() {
    return '[' + this._min + '-' + this._max + ']';
}

function _Compound(ranges) {
    this._ranges = ranges;
    // assert sorted?
}

_Compound.prototype.min = function() {
    return this._ranges[0].min();
}

_Compound.prototype.max = function() {
    return this._ranges[this._ranges.length - 1].max();
}

_Compound.prototype.contains = function(pos) {
    // FIXME implement bsearch if we use this much.
    for (var s = 0; s < this._ranges.length; ++s) {
        if (this._ranges[s].contains(pos)) {
            return true;
        }
    }
    return false;
}

_Compound.prototype.isContiguous = function() {
    return this._ranges.length > 1;
}

_Compound.prototype.ranges = function() {
    return this._ranges;
}

_Compound.prototype._pushRanges = function(ranges) {
    for (var ri = 0; ri < this._ranges.length; ++ri)
        ranges.push(this._ranges[ri]);
}

_Compound.prototype.toString = function() {
    var s = '';
    for (var r = 0; r < this._ranges.length; ++r) {
        if (r>0) {
            s = s + ',';
        }
        s = s + this._ranges[r].toString();
    }
    return s;
}

function union(s0, s1) {
    if (! (s0 instanceof Array)) {
        s0 = [s0];
        if (s1)
            s0.push(s1);
    }

    if (s0.length == 0)
        return null;
    else if (s0.length == 1)
        return s0[0];

    var ranges = [];
    for (var si = 0; si < s0.length; ++si)
        s0[si]._pushRanges(ranges);
    ranges = ranges.sort(_rangeOrder);

    var oranges = [];
    var current = ranges[0];
    current = new Range(current._min, current._max);  // Copy now so we don't have to later.

    for (var i = 1; i < ranges.length; ++i) {
        var nxt = ranges[i];
        if (nxt._min > (current._max + 1)) {
            oranges.push(current);
            current = new Range(nxt._min, nxt._max);
        } else {
            if (nxt._max > current._max) {
                current._max = nxt._max;
            }
        }
    }
    oranges.push(current);

    if (oranges.length == 1) {
        return oranges[0];
    } else {
        return new _Compound(oranges);
    }
}

function intersection(s0, s1) {
    var r0 = s0.ranges();
    var r1 = s1.ranges();
    var l0 = r0.length, l1 = r1.length;
    var i0 = 0, i1 = 0;
    var or = [];

    while (i0 < l0 && i1 < l1) {
        var s0 = r0[i0], s1 = r1[i1];
        var lapMin = Math.max(s0.min(), s1.min());
        var lapMax = Math.min(s0.max(), s1.max());
        if (lapMax >= lapMin) {
            or.push(new Range(lapMin, lapMax));
        }
        if (s0.max() > s1.max()) {
            ++i1;
        } else {
            ++i0;
        }
    }
    
    if (or.length == 0) {
        return null; // FIXME
    } else if (or.length == 1) {
        return or[0];
    } else {
        return new _Compound(or);
    }
}

function coverage(s) {
    var tot = 0;
    var rl = s.ranges();
    for (var ri = 0; ri < rl.length; ++ri) {
        var r = rl[ri];
        tot += (r.max() - r.min() + 1);
    }
    return tot;
}



function rangeOrder(a, b)
{
    if (a.min() < b.min()) {
        return -1;
    } else if (a.min() > b.min()) {
        return 1;
    } else if (a.max() < b.max()) {
        return -1;
    } else if (b.max() > a.max()) {
        return 1;
    } else {
        return 0;
    }
}

function _rangeOrder(a, b)
{
    if (a._min < b._min) {
        return -1;
    } else if (a._min > b._min) {
        return 1;
    } else if (a._max < b._max) {
        return -1;
    } else if (b._max > a._max) {
        return 1;
    } else {
        return 0;
    }
}

if (typeof(module) !== 'undefined') {
    module.exports = {
        Range: Range,
        union: union,
        intersection: intersection,
        coverage: coverage,
        rangeOver: rangeOrder,
        _rangeOrder: _rangeOrder
    }
}
},{}],10:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2010
//
// utils.js: odds, sods, and ends.
//

"use strict";

if (typeof(require) !== 'undefined') {
    var sha1 = require('./sha1');
    var b64_sha1 = sha1.b64_sha1;
}

var NUM_REGEXP = new RegExp('[0-9]+');

function stringToNumbersArray(str) {
    var nums = new Array();
    var m;
    while (m = NUM_REGEXP.exec(str)) {
        nums.push(m[0]);
        str=str.substring(m.index + (m[0].length));
    }
    return nums;
}

var STRICT_NUM_REGEXP = new RegExp('^[0-9]+$');

function stringToInt(str) {
    str = str.replace(new RegExp(',', 'g'), '');
    if (!STRICT_NUM_REGEXP.test(str)) {
        return null;
    }
    return str|0;
}

function pushnew(a, v) {
    for (var i = 0; i < a.length; ++i) {
        if (a[i] == v) {
            return;
        }
    }
    a.push(v);
}

function pusho(obj, k, v) {
    if (obj[k]) {
        obj[k].push(v);
    } else {
        obj[k] = [v];
    }
}

function pushnewo(obj, k, v) {
    var a = obj[k];
    if (a) {
        for (var i = 0; i < a.length; ++i) {    // indexOf requires JS16 :-(.
            if (a[i] == v) {
                return;
            }
        }
        a.push(v);
    } else {
        obj[k] = [v];
    }
}


function pick(a, b, c, d)
{
    if (a) {
        return a;
    } else if (b) {
        return b;
    } else if (c) {
        return c;
    } else if (d) {
        return d;
    }
}

function pushnew(l, o)
{
    for (var i = 0; i < l.length; ++i) {
        if (l[i] == o) {
            return;
        }
    }
    l.push(o);
}



function arrayIndexOf(a, x) {
    if (!a) {
        return -1;
    }

    for (var i = 0; i < a.length; ++i) {
        if (a[i] === x) {
            return i;
        }
    }
    return -1;
}

function arrayRemove(a, x) {
    var i = arrayIndexOf(a, x);
    if (i >= 0) {
        a.splice(i, 1);
        return true;
    }
    return false;
}

//
// DOM utilities
//


function makeElement(tag, children, attribs, styles)
{
    var ele = document.createElement(tag);
    if (children) {
        if (! (children instanceof Array)) {
            children = [children];
        }
        for (var i = 0; i < children.length; ++i) {
            var c = children[i];
            if (typeof c == 'string') {
                c = document.createTextNode(c);
            }
            ele.appendChild(c);
        }
    }
    
    if (attribs) {
        for (var l in attribs) {
            try {
                ele[l] = attribs[l];
            } catch (e) {
                console.log('error setting ' + l);
                throw(e);
            }
        }
    }
    if (styles) {
        for (var l in styles) {
            ele.style[l] = styles[l];
        }
    }
    return ele;
}

function makeElementNS(namespace, tag, children, attribs)
{
    var ele = document.createElementNS(namespace, tag);
    if (children) {
        if (! (children instanceof Array)) {
            children = [children];
        }
        for (var i = 0; i < children.length; ++i) {
            var c = children[i];
            if (typeof c == 'string') {
                c = document.createTextNode(c);
            }
            ele.appendChild(c);
        }
    }
    
    setAttrs(ele, attribs);
    return ele;
}

var attr_name_cache = {};

function setAttr(node, key, value)
{
    var attr = attr_name_cache[key];
    if (!attr) {
        var _attr = '';
        for (var c = 0; c < key.length; ++c) {
            var cc = key.substring(c, c+1);
            var lcc = cc.toLowerCase();
            if (lcc != cc) {
                _attr = _attr + '-' + lcc;
            } else {
                _attr = _attr + cc;
            }
        }
        attr_name_cache[key] = _attr;
        attr = _attr;
    }
    node.setAttribute(attr, value);
}

function setAttrs(node, attribs)
{
    if (attribs) {
        for (var l in attribs) {
            setAttr(node, l, attribs[l]);
        }
    }
}



function removeChildren(node)
{
    if (!node || !node.childNodes) {
        return;
    }

    while (node.childNodes.length > 0) {
        node.removeChild(node.firstChild);
    }
}



//
// WARNING: not for general use!
//

function miniJSONify(o, exc) {
    if (typeof o === 'undefined') {
        return 'undefined';
    } else if (o == null) {
        return 'null';
    } else if (typeof o == 'string') {
        return "'" + o + "'";
    } else if (typeof o == 'number') {
        return "" + o;
    } else if (typeof o == 'boolean') {
        return "" + o;
    } else if (typeof o == 'object') {
        if (o instanceof Array) {
            var s = null;
            for (var i = 0; i < o.length; ++i) {
                s = (s == null ? '' : (s + ', ')) + miniJSONify(o[i], exc);
            }
            return '[' + (s?s:'') + ']';
        } else {
            exc = exc || {};
            var s = null;
            for (var k in o) {
                if (exc[k])
                    continue;
                if (k != undefined && typeof(o[k]) != 'function') {
                    s = (s == null ? '' : (s + ', ')) + k + ': ' + miniJSONify(o[k], exc);
                }
            }
            return '{' + (s?s:'') + '}';
        }
    } else {
        return (typeof o);
    }
}

function shallowCopy(o) {
    var n = {};
    for (var k in o) {
        n[k] = o[k];
    }
    return n;
}

function Observed(x) {
    this.value = x;
    this.listeners = [];
}

Observed.prototype.addListener = function(f) {
    this.listeners.push(f);
}

Observed.prototype.addListenerAndFire = function(f) {
    this.listeners.push(f);
    f(this.value);
}

Observed.prototype.removeListener = function(f) {
    arrayRemove(this.listeners, f);
}

Observed.prototype.get = function() {
    return this.value;
}

Observed.prototype.set = function(x) {
    this.value = x;
    for (var i = 0; i < this.listeners.length; ++i) {
        this.listeners[i](x);
    }
}

function Awaited() {
    this.queue = [];
}

Awaited.prototype.provide = function(x) {
    if (this.res !== undefined) {
        throw "Resource has already been provided.";
    }

    this.res = x;
    for (var i = 0; i < this.queue.length; ++i) {
        this.queue[i](x);
    }
    this.queue = null;   // avoid leaking closures.
}

Awaited.prototype.await = function(f) {
    if (this.res !== undefined) {
        f(this.res);
        return this.res;
    } else {
        this.queue.push(f);
    }
}

var __dalliance_saltSeed = 0;

function saltURL(url) {
    return url + '?salt=' + b64_sha1('' + Date.now() + ',' + (++__dalliance_saltSeed));
}

function textXHR(url, callback, opts) {
    if (opts.salt) 
        url = saltURL(url);

    var req = new XMLHttpRequest();
    req.onreadystatechange = function() {
    	if (req.readyState == 4) {
    	    if (req.status >= 300) {
    		    callback(null, 'Error code ' + req.status);
    	    } else {
    		    callback(req.responseText);
    	    }
    	}
    };
    
    req.open('GET', url, true);
    req.responseType = 'text';

    if (opts && opts.credentials) {
        req.withCredentials = true;
    }
    req.send('');
}

function relativeURL(base, rel) {
    // FIXME quite naive -- good enough for trackhubs?

    if (rel.indexOf('http:') == 0 || rel.indexOf('https:') == 0) {
        return rel;
    }

    var li = base.lastIndexOf('/');
    if (li >= 0) {
        return base.substr(0, li + 1) + rel;
    } else {
        return rel;
    }
}

//
// Missing APIs
// 

if (!('trim' in String.prototype)) {
    String.prototype.trim = function() {
        return this.replace(/^\s+/, '').replace(/\s+$/, '');
    };
}

if (typeof(module) !== 'undefined') {
    module.exports = {
        textXHR: textXHR,
        relativeURL: relativeURL,
        shallowCopy: shallowCopy,
        pusho: pusho,
        pushnew: pushnew,
        pushnewo: pushnewo,
        arrayIndexOf: arrayIndexOf,
        pick: pick,

        makeElement: makeElement,
        makeElementNS: makeElementNS,
        removeChildren: removeChildren,

        miniJSONify: miniJSONify,

        Observed: Observed,
        Awaited: Awaited
    }
}

},{"./sha1":8}],11:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Javascript ZLib
// By Thomas Down 2010-2011
//
// Based very heavily on portions of jzlib (by ymnk@jcraft.com), who in
// turn credits Jean-loup Gailly and Mark Adler for the original zlib code.
//
// inflate.js: ZLib inflate code
//

//
// Shared constants
//

var MAX_WBITS=15; // 32K LZ77 window
var DEF_WBITS=MAX_WBITS;
var MAX_MEM_LEVEL=9;
var MANY=1440;
var BMAX = 15;

// preset dictionary flag in zlib header
var PRESET_DICT=0x20;

var Z_NO_FLUSH=0;
var Z_PARTIAL_FLUSH=1;
var Z_SYNC_FLUSH=2;
var Z_FULL_FLUSH=3;
var Z_FINISH=4;

var Z_DEFLATED=8;

var Z_OK=0;
var Z_STREAM_END=1;
var Z_NEED_DICT=2;
var Z_ERRNO=-1;
var Z_STREAM_ERROR=-2;
var Z_DATA_ERROR=-3;
var Z_MEM_ERROR=-4;
var Z_BUF_ERROR=-5;
var Z_VERSION_ERROR=-6;

var METHOD=0;   // waiting for method byte
var FLAG=1;     // waiting for flag byte
var DICT4=2;    // four dictionary check bytes to go
var DICT3=3;    // three dictionary check bytes to go
var DICT2=4;    // two dictionary check bytes to go
var DICT1=5;    // one dictionary check byte to go
var DICT0=6;    // waiting for inflateSetDictionary
var BLOCKS=7;   // decompressing blocks
var CHECK4=8;   // four check bytes to go
var CHECK3=9;   // three check bytes to go
var CHECK2=10;  // two check bytes to go
var CHECK1=11;  // one check byte to go
var DONE=12;    // finished check, done
var BAD=13;     // got an error--stay here

var inflate_mask = [0x00000000, 0x00000001, 0x00000003, 0x00000007, 0x0000000f, 0x0000001f, 0x0000003f, 0x0000007f, 0x000000ff, 0x000001ff, 0x000003ff, 0x000007ff, 0x00000fff, 0x00001fff, 0x00003fff, 0x00007fff, 0x0000ffff];

var IB_TYPE=0;  // get type bits (3, including end bit)
var IB_LENS=1;  // get lengths for stored
var IB_STORED=2;// processing stored block
var IB_TABLE=3; // get table lengths
var IB_BTREE=4; // get bit lengths tree for a dynamic block
var IB_DTREE=5; // get length, distance trees for a dynamic block
var IB_CODES=6; // processing fixed or dynamic block
var IB_DRY=7;   // output remaining window bytes
var IB_DONE=8;  // finished last block, done
var IB_BAD=9;   // ot a data error--stuck here

var fixed_bl = 9;
var fixed_bd = 5;

var fixed_tl = [
    96,7,256, 0,8,80, 0,8,16, 84,8,115,
    82,7,31, 0,8,112, 0,8,48, 0,9,192,
    80,7,10, 0,8,96, 0,8,32, 0,9,160,
    0,8,0, 0,8,128, 0,8,64, 0,9,224,
    80,7,6, 0,8,88, 0,8,24, 0,9,144,
    83,7,59, 0,8,120, 0,8,56, 0,9,208,
    81,7,17, 0,8,104, 0,8,40, 0,9,176,
    0,8,8, 0,8,136, 0,8,72, 0,9,240,
    80,7,4, 0,8,84, 0,8,20, 85,8,227,
    83,7,43, 0,8,116, 0,8,52, 0,9,200,
    81,7,13, 0,8,100, 0,8,36, 0,9,168,
    0,8,4, 0,8,132, 0,8,68, 0,9,232,
    80,7,8, 0,8,92, 0,8,28, 0,9,152,
    84,7,83, 0,8,124, 0,8,60, 0,9,216,
    82,7,23, 0,8,108, 0,8,44, 0,9,184,
    0,8,12, 0,8,140, 0,8,76, 0,9,248,
    80,7,3, 0,8,82, 0,8,18, 85,8,163,
    83,7,35, 0,8,114, 0,8,50, 0,9,196,
    81,7,11, 0,8,98, 0,8,34, 0,9,164,
    0,8,2, 0,8,130, 0,8,66, 0,9,228,
    80,7,7, 0,8,90, 0,8,26, 0,9,148,
    84,7,67, 0,8,122, 0,8,58, 0,9,212,
    82,7,19, 0,8,106, 0,8,42, 0,9,180,
    0,8,10, 0,8,138, 0,8,74, 0,9,244,
    80,7,5, 0,8,86, 0,8,22, 192,8,0,
    83,7,51, 0,8,118, 0,8,54, 0,9,204,
    81,7,15, 0,8,102, 0,8,38, 0,9,172,
    0,8,6, 0,8,134, 0,8,70, 0,9,236,
    80,7,9, 0,8,94, 0,8,30, 0,9,156,
    84,7,99, 0,8,126, 0,8,62, 0,9,220,
    82,7,27, 0,8,110, 0,8,46, 0,9,188,
    0,8,14, 0,8,142, 0,8,78, 0,9,252,
    96,7,256, 0,8,81, 0,8,17, 85,8,131,
    82,7,31, 0,8,113, 0,8,49, 0,9,194,
    80,7,10, 0,8,97, 0,8,33, 0,9,162,
    0,8,1, 0,8,129, 0,8,65, 0,9,226,
    80,7,6, 0,8,89, 0,8,25, 0,9,146,
    83,7,59, 0,8,121, 0,8,57, 0,9,210,
    81,7,17, 0,8,105, 0,8,41, 0,9,178,
    0,8,9, 0,8,137, 0,8,73, 0,9,242,
    80,7,4, 0,8,85, 0,8,21, 80,8,258,
    83,7,43, 0,8,117, 0,8,53, 0,9,202,
    81,7,13, 0,8,101, 0,8,37, 0,9,170,
    0,8,5, 0,8,133, 0,8,69, 0,9,234,
    80,7,8, 0,8,93, 0,8,29, 0,9,154,
    84,7,83, 0,8,125, 0,8,61, 0,9,218,
    82,7,23, 0,8,109, 0,8,45, 0,9,186,
    0,8,13, 0,8,141, 0,8,77, 0,9,250,
    80,7,3, 0,8,83, 0,8,19, 85,8,195,
    83,7,35, 0,8,115, 0,8,51, 0,9,198,
    81,7,11, 0,8,99, 0,8,35, 0,9,166,
    0,8,3, 0,8,131, 0,8,67, 0,9,230,
    80,7,7, 0,8,91, 0,8,27, 0,9,150,
    84,7,67, 0,8,123, 0,8,59, 0,9,214,
    82,7,19, 0,8,107, 0,8,43, 0,9,182,
    0,8,11, 0,8,139, 0,8,75, 0,9,246,
    80,7,5, 0,8,87, 0,8,23, 192,8,0,
    83,7,51, 0,8,119, 0,8,55, 0,9,206,
    81,7,15, 0,8,103, 0,8,39, 0,9,174,
    0,8,7, 0,8,135, 0,8,71, 0,9,238,
    80,7,9, 0,8,95, 0,8,31, 0,9,158,
    84,7,99, 0,8,127, 0,8,63, 0,9,222,
    82,7,27, 0,8,111, 0,8,47, 0,9,190,
    0,8,15, 0,8,143, 0,8,79, 0,9,254,
    96,7,256, 0,8,80, 0,8,16, 84,8,115,
    82,7,31, 0,8,112, 0,8,48, 0,9,193,

    80,7,10, 0,8,96, 0,8,32, 0,9,161,
    0,8,0, 0,8,128, 0,8,64, 0,9,225,
    80,7,6, 0,8,88, 0,8,24, 0,9,145,
    83,7,59, 0,8,120, 0,8,56, 0,9,209,
    81,7,17, 0,8,104, 0,8,40, 0,9,177,
    0,8,8, 0,8,136, 0,8,72, 0,9,241,
    80,7,4, 0,8,84, 0,8,20, 85,8,227,
    83,7,43, 0,8,116, 0,8,52, 0,9,201,
    81,7,13, 0,8,100, 0,8,36, 0,9,169,
    0,8,4, 0,8,132, 0,8,68, 0,9,233,
    80,7,8, 0,8,92, 0,8,28, 0,9,153,
    84,7,83, 0,8,124, 0,8,60, 0,9,217,
    82,7,23, 0,8,108, 0,8,44, 0,9,185,
    0,8,12, 0,8,140, 0,8,76, 0,9,249,
    80,7,3, 0,8,82, 0,8,18, 85,8,163,
    83,7,35, 0,8,114, 0,8,50, 0,9,197,
    81,7,11, 0,8,98, 0,8,34, 0,9,165,
    0,8,2, 0,8,130, 0,8,66, 0,9,229,
    80,7,7, 0,8,90, 0,8,26, 0,9,149,
    84,7,67, 0,8,122, 0,8,58, 0,9,213,
    82,7,19, 0,8,106, 0,8,42, 0,9,181,
    0,8,10, 0,8,138, 0,8,74, 0,9,245,
    80,7,5, 0,8,86, 0,8,22, 192,8,0,
    83,7,51, 0,8,118, 0,8,54, 0,9,205,
    81,7,15, 0,8,102, 0,8,38, 0,9,173,
    0,8,6, 0,8,134, 0,8,70, 0,9,237,
    80,7,9, 0,8,94, 0,8,30, 0,9,157,
    84,7,99, 0,8,126, 0,8,62, 0,9,221,
    82,7,27, 0,8,110, 0,8,46, 0,9,189,
    0,8,14, 0,8,142, 0,8,78, 0,9,253,
    96,7,256, 0,8,81, 0,8,17, 85,8,131,
    82,7,31, 0,8,113, 0,8,49, 0,9,195,
    80,7,10, 0,8,97, 0,8,33, 0,9,163,
    0,8,1, 0,8,129, 0,8,65, 0,9,227,
    80,7,6, 0,8,89, 0,8,25, 0,9,147,
    83,7,59, 0,8,121, 0,8,57, 0,9,211,
    81,7,17, 0,8,105, 0,8,41, 0,9,179,
    0,8,9, 0,8,137, 0,8,73, 0,9,243,
    80,7,4, 0,8,85, 0,8,21, 80,8,258,
    83,7,43, 0,8,117, 0,8,53, 0,9,203,
    81,7,13, 0,8,101, 0,8,37, 0,9,171,
    0,8,5, 0,8,133, 0,8,69, 0,9,235,
    80,7,8, 0,8,93, 0,8,29, 0,9,155,
    84,7,83, 0,8,125, 0,8,61, 0,9,219,
    82,7,23, 0,8,109, 0,8,45, 0,9,187,
    0,8,13, 0,8,141, 0,8,77, 0,9,251,
    80,7,3, 0,8,83, 0,8,19, 85,8,195,
    83,7,35, 0,8,115, 0,8,51, 0,9,199,
    81,7,11, 0,8,99, 0,8,35, 0,9,167,
    0,8,3, 0,8,131, 0,8,67, 0,9,231,
    80,7,7, 0,8,91, 0,8,27, 0,9,151,
    84,7,67, 0,8,123, 0,8,59, 0,9,215,
    82,7,19, 0,8,107, 0,8,43, 0,9,183,
    0,8,11, 0,8,139, 0,8,75, 0,9,247,
    80,7,5, 0,8,87, 0,8,23, 192,8,0,
    83,7,51, 0,8,119, 0,8,55, 0,9,207,
    81,7,15, 0,8,103, 0,8,39, 0,9,175,
    0,8,7, 0,8,135, 0,8,71, 0,9,239,
    80,7,9, 0,8,95, 0,8,31, 0,9,159,
    84,7,99, 0,8,127, 0,8,63, 0,9,223,
    82,7,27, 0,8,111, 0,8,47, 0,9,191,
    0,8,15, 0,8,143, 0,8,79, 0,9,255
];
var fixed_td = [
    80,5,1, 87,5,257, 83,5,17, 91,5,4097,
    81,5,5, 89,5,1025, 85,5,65, 93,5,16385,
    80,5,3, 88,5,513, 84,5,33, 92,5,8193,
    82,5,9, 90,5,2049, 86,5,129, 192,5,24577,
    80,5,2, 87,5,385, 83,5,25, 91,5,6145,
    81,5,7, 89,5,1537, 85,5,97, 93,5,24577,
    80,5,4, 88,5,769, 84,5,49, 92,5,12289,
    82,5,13, 90,5,3073, 86,5,193, 192,5,24577
];

  // Tables for deflate from PKZIP's appnote.txt.
  var cplens = [ // Copy lengths for literal codes 257..285
        3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
        35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0
  ];

  // see note #13 above about 258
  var cplext = [ // Extra bits for literal codes 257..285
        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
        3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0, 112, 112  // 112==invalid
  ];

 var cpdist = [ // Copy offsets for distance codes 0..29
        1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
        257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145,
        8193, 12289, 16385, 24577
  ];

  var cpdext = [ // Extra bits for distance codes
        0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
        7, 7, 8, 8, 9, 9, 10, 10, 11, 11,
        12, 12, 13, 13];

//
// ZStream.java
//

function ZStream() {
}


ZStream.prototype.inflateInit = function(w, nowrap) {
    if (!w) {
	w = DEF_WBITS;
    }
    if (nowrap) {
	nowrap = false;
    }
    this.istate = new Inflate();
    return this.istate.inflateInit(this, nowrap?-w:w);
}

ZStream.prototype.inflate = function(f) {
    if(this.istate==null) return Z_STREAM_ERROR;
    return this.istate.inflate(this, f);
}

ZStream.prototype.inflateEnd = function(){
    if(this.istate==null) return Z_STREAM_ERROR;
    var ret=istate.inflateEnd(this);
    this.istate = null;
    return ret;
}
ZStream.prototype.inflateSync = function(){
    // if(istate == null) return Z_STREAM_ERROR;
    return istate.inflateSync(this);
}
ZStream.prototype.inflateSetDictionary = function(dictionary, dictLength){
    // if(istate == null) return Z_STREAM_ERROR;
    return istate.inflateSetDictionary(this, dictionary, dictLength);
}

/*

  public int deflateInit(int level){
    return deflateInit(level, MAX_WBITS);
  }
  public int deflateInit(int level, boolean nowrap){
    return deflateInit(level, MAX_WBITS, nowrap);
  }
  public int deflateInit(int level, int bits){
    return deflateInit(level, bits, false);
  }
  public int deflateInit(int level, int bits, boolean nowrap){
    dstate=new Deflate();
    return dstate.deflateInit(this, level, nowrap?-bits:bits);
  }
  public int deflate(int flush){
    if(dstate==null){
      return Z_STREAM_ERROR;
    }
    return dstate.deflate(this, flush);
  }
  public int deflateEnd(){
    if(dstate==null) return Z_STREAM_ERROR;
    int ret=dstate.deflateEnd();
    dstate=null;
    return ret;
  }
  public int deflateParams(int level, int strategy){
    if(dstate==null) return Z_STREAM_ERROR;
    return dstate.deflateParams(this, level, strategy);
  }
  public int deflateSetDictionary (byte[] dictionary, int dictLength){
    if(dstate == null)
      return Z_STREAM_ERROR;
    return dstate.deflateSetDictionary(this, dictionary, dictLength);
  }

*/

/*
  // Flush as much pending output as possible. All deflate() output goes
  // through this function so some applications may wish to modify it
  // to avoid allocating a large strm->next_out buffer and copying into it.
  // (See also read_buf()).
  void flush_pending(){
    int len=dstate.pending;

    if(len>avail_out) len=avail_out;
    if(len==0) return;

    if(dstate.pending_buf.length<=dstate.pending_out ||
       next_out.length<=next_out_index ||
       dstate.pending_buf.length<(dstate.pending_out+len) ||
       next_out.length<(next_out_index+len)){
      System.out.println(dstate.pending_buf.length+", "+dstate.pending_out+
			 ", "+next_out.length+", "+next_out_index+", "+len);
      System.out.println("avail_out="+avail_out);
    }

    System.arraycopy(dstate.pending_buf, dstate.pending_out,
		     next_out, next_out_index, len);

    next_out_index+=len;
    dstate.pending_out+=len;
    total_out+=len;
    avail_out-=len;
    dstate.pending-=len;
    if(dstate.pending==0){
      dstate.pending_out=0;
    }
  }

  // Read a new buffer from the current input stream, update the adler32
  // and total number of bytes read.  All deflate() input goes through
  // this function so some applications may wish to modify it to avoid
  // allocating a large strm->next_in buffer and copying from it.
  // (See also flush_pending()).
  int read_buf(byte[] buf, int start, int size) {
    int len=avail_in;

    if(len>size) len=size;
    if(len==0) return 0;

    avail_in-=len;

    if(dstate.noheader==0) {
      adler=_adler.adler32(adler, next_in, next_in_index, len);
    }
    System.arraycopy(next_in, next_in_index, buf, start, len);
    next_in_index  += len;
    total_in += len;
    return len;
  }

  public void free(){
    next_in=null;
    next_out=null;
    msg=null;
    _adler=null;
  }
}
*/


//
// Inflate.java
//

function Inflate() {
    this.was = [0];
}

Inflate.prototype.inflateReset = function(z) {
    if(z == null || z.istate == null) return Z_STREAM_ERROR;
    
    z.total_in = z.total_out = 0;
    z.msg = null;
    z.istate.mode = z.istate.nowrap!=0 ? BLOCKS : METHOD;
    z.istate.blocks.reset(z, null);
    return Z_OK;
}

Inflate.prototype.inflateEnd = function(z){
    if(this.blocks != null)
      this.blocks.free(z);
    this.blocks=null;
    return Z_OK;
}

Inflate.prototype.inflateInit = function(z, w){
    z.msg = null;
    this.blocks = null;

    // handle undocumented nowrap option (no zlib header or check)
    nowrap = 0;
    if(w < 0){
      w = - w;
      nowrap = 1;
    }

    // set window size
    if(w<8 ||w>15){
      this.inflateEnd(z);
      return Z_STREAM_ERROR;
    }
    this.wbits=w;

    z.istate.blocks=new InfBlocks(z, 
				  z.istate.nowrap!=0 ? null : this,
				  1<<w);

    // reset state
    this.inflateReset(z);
    return Z_OK;
  }

Inflate.prototype.inflate = function(z, f){
    var r, b;

    if(z == null || z.istate == null || z.next_in == null)
      return Z_STREAM_ERROR;
    f = f == Z_FINISH ? Z_BUF_ERROR : Z_OK;
    r = Z_BUF_ERROR;
    while (true){
      switch (z.istate.mode){
      case METHOD:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        if(((z.istate.method = z.next_in[z.next_in_index++])&0xf)!=Z_DEFLATED){
          z.istate.mode = BAD;
          z.msg="unknown compression method";
          z.istate.marker = 5;       // can't try inflateSync
          break;
        }
        if((z.istate.method>>4)+8>z.istate.wbits){
          z.istate.mode = BAD;
          z.msg="invalid window size";
          z.istate.marker = 5;       // can't try inflateSync
          break;
        }
        z.istate.mode=FLAG;
      case FLAG:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        b = (z.next_in[z.next_in_index++])&0xff;

        if((((z.istate.method << 8)+b) % 31)!=0){
          z.istate.mode = BAD;
          z.msg = "incorrect header check";
          z.istate.marker = 5;       // can't try inflateSync
          break;
        }

        if((b&PRESET_DICT)==0){
          z.istate.mode = BLOCKS;
          break;
        }
        z.istate.mode = DICT4;
      case DICT4:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need=((z.next_in[z.next_in_index++]&0xff)<<24)&0xff000000;
        z.istate.mode=DICT3;
      case DICT3:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need+=((z.next_in[z.next_in_index++]&0xff)<<16)&0xff0000;
        z.istate.mode=DICT2;
      case DICT2:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need+=((z.next_in[z.next_in_index++]&0xff)<<8)&0xff00;
        z.istate.mode=DICT1;
      case DICT1:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need += (z.next_in[z.next_in_index++]&0xff);
        z.adler = z.istate.need;
        z.istate.mode = DICT0;
        return Z_NEED_DICT;
      case DICT0:
        z.istate.mode = BAD;
        z.msg = "need dictionary";
        z.istate.marker = 0;       // can try inflateSync
        return Z_STREAM_ERROR;
      case BLOCKS:

        r = z.istate.blocks.proc(z, r);
        if(r == Z_DATA_ERROR){
          z.istate.mode = BAD;
          z.istate.marker = 0;     // can try inflateSync
          break;
        }
        if(r == Z_OK){
          r = f;
        }
        if(r != Z_STREAM_END){
          return r;
        }
        r = f;
        z.istate.blocks.reset(z, z.istate.was);
        if(z.istate.nowrap!=0){
          z.istate.mode=DONE;
          break;
        }
        z.istate.mode=CHECK4;
      case CHECK4:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need=((z.next_in[z.next_in_index++]&0xff)<<24)&0xff000000;
        z.istate.mode=CHECK3;
      case CHECK3:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need+=((z.next_in[z.next_in_index++]&0xff)<<16)&0xff0000;
        z.istate.mode = CHECK2;
      case CHECK2:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need+=((z.next_in[z.next_in_index++]&0xff)<<8)&0xff00;
        z.istate.mode = CHECK1;
      case CHECK1:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need+=(z.next_in[z.next_in_index++]&0xff);

        if(((z.istate.was[0])) != ((z.istate.need))){
          z.istate.mode = BAD;
          z.msg = "incorrect data check";
          z.istate.marker = 5;       // can't try inflateSync
          break;
        }

        z.istate.mode = DONE;
      case DONE:
        return Z_STREAM_END;
      case BAD:
        return Z_DATA_ERROR;
      default:
        return Z_STREAM_ERROR;
      }
    }
  }


Inflate.prototype.inflateSetDictionary = function(z,  dictionary, dictLength) {
    var index=0;
    var length = dictLength;
    if(z==null || z.istate == null|| z.istate.mode != DICT0)
      return Z_STREAM_ERROR;

    if(z._adler.adler32(1, dictionary, 0, dictLength)!=z.adler){
      return Z_DATA_ERROR;
    }

    z.adler = z._adler.adler32(0, null, 0, 0);

    if(length >= (1<<z.istate.wbits)){
      length = (1<<z.istate.wbits)-1;
      index=dictLength - length;
    }
    z.istate.blocks.set_dictionary(dictionary, index, length);
    z.istate.mode = BLOCKS;
    return Z_OK;
  }

//  static private byte[] mark = {(byte)0, (byte)0, (byte)0xff, (byte)0xff};
var mark = [0, 0, 255, 255]

Inflate.prototype.inflateSync = function(z){
    var n;       // number of bytes to look at
    var p;       // pointer to bytes
    var m;       // number of marker bytes found in a row
    var r, w;   // temporaries to save total_in and total_out

    // set up
    if(z == null || z.istate == null)
      return Z_STREAM_ERROR;
    if(z.istate.mode != BAD){
      z.istate.mode = BAD;
      z.istate.marker = 0;
    }
    if((n=z.avail_in)==0)
      return Z_BUF_ERROR;
    p=z.next_in_index;
    m=z.istate.marker;

    // search
    while (n!=0 && m < 4){
      if(z.next_in[p] == mark[m]){
        m++;
      }
      else if(z.next_in[p]!=0){
        m = 0;
      }
      else{
        m = 4 - m;
      }
      p++; n--;
    }

    // restore
    z.total_in += p-z.next_in_index;
    z.next_in_index = p;
    z.avail_in = n;
    z.istate.marker = m;

    // return no joy or set up to restart on a new block
    if(m != 4){
      return Z_DATA_ERROR;
    }
    r=z.total_in;  w=z.total_out;
    this.inflateReset(z);
    z.total_in=r;  z.total_out = w;
    z.istate.mode = BLOCKS;
    return Z_OK;
}

  // Returns true if inflate is currently at the end of a block generated
  // by Z_SYNC_FLUSH or Z_FULL_FLUSH. This function is used by one PPP
  // implementation to provide an additional safety check. PPP uses Z_SYNC_FLUSH
  // but removes the length bytes of the resulting empty stored block. When
  // decompressing, PPP checks that at the end of input packet, inflate is
  // waiting for these length bytes.
Inflate.prototype.inflateSyncPoint = function(z){
    if(z == null || z.istate == null || z.istate.blocks == null)
      return Z_STREAM_ERROR;
    return z.istate.blocks.sync_point();
}


//
// InfBlocks.java
//

var INFBLOCKS_BORDER = [16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15];

function InfBlocks(z, checkfn, w) {
    this.hufts=new Int32Array(MANY*3);
    this.window=new Uint8Array(w);
    this.end=w;
    this.checkfn = checkfn;
    this.mode = IB_TYPE;
    this.reset(z, null);

    this.left = 0;            // if STORED, bytes left to copy 

    this.table = 0;           // table lengths (14 bits) 
    this.index = 0;           // index into blens (or border) 
    this.blens = null;         // bit lengths of codes 
    this.bb=new Int32Array(1); // bit length tree depth 
    this.tb=new Int32Array(1); // bit length decoding tree 

    this.codes = new InfCodes();

    this.last = 0;            // true if this block is the last block 

  // mode independent information 
    this.bitk = 0;            // bits in bit buffer 
    this.bitb = 0;            // bit buffer 
    this.read = 0;            // window read pointer 
    this.write = 0;           // window write pointer 
    this.check = 0;          // check on output 

    this.inftree=new InfTree();
}




InfBlocks.prototype.reset = function(z, c){
    if(c) c[0]=this.check;
    if(this.mode==IB_CODES){
      this.codes.free(z);
    }
    this.mode=IB_TYPE;
    this.bitk=0;
    this.bitb=0;
    this.read=this.write=0;

    if(this.checkfn)
      z.adler=this.check=z._adler.adler32(0, null, 0, 0);
  }

 InfBlocks.prototype.proc = function(z, r){
    var t;              // temporary storage
    var b;              // bit buffer
    var k;              // bits in bit buffer
    var p;              // input data pointer
    var n;              // bytes available there
    var q;              // output window write pointer
    var m;              // bytes to end of window or read pointer

    // copy input/output information to locals (UPDATE macro restores)
    {p=z.next_in_index;n=z.avail_in;b=this.bitb;k=this.bitk;}
    {q=this.write;m=(q<this.read ? this.read-q-1 : this.end-q);}

    // process input based on current state
    while(true){
      switch (this.mode){
      case IB_TYPE:

	while(k<(3)){
	  if(n!=0){
	    r=Z_OK;
	  }
	  else{
	    this.bitb=b; this.bitk=k; 
	    z.avail_in=n;
	    z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    this.write=q;
	    return this.inflate_flush(z,r);
	  };
	  n--;
	  b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}
	t = (b & 7);
	this.last = t & 1;

	switch (t >>> 1){
        case 0:                         // stored 
          {b>>>=(3);k-=(3);}
          t = k & 7;                    // go to byte boundary

          {b>>>=(t);k-=(t);}
          this.mode = IB_LENS;                  // get length of stored block
          break;
        case 1:                         // fixed
          {
              var bl=new Int32Array(1);
	      var bd=new Int32Array(1);
              var tl=[];
	      var td=[];

	      inflate_trees_fixed(bl, bd, tl, td, z);
              this.codes.init(bl[0], bd[0], tl[0], 0, td[0], 0, z);
          }

          {b>>>=(3);k-=(3);}

          this.mode = IB_CODES;
          break;
        case 2:                         // dynamic

          {b>>>=(3);k-=(3);}

          this.mode = IB_TABLE;
          break;
        case 3:                         // illegal

          {b>>>=(3);k-=(3);}
          this.mode = BAD;
          z.msg = "invalid block type";
          r = Z_DATA_ERROR;

	  this.bitb=b; this.bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  this.write=q;
	  return this.inflate_flush(z,r);
	}
	break;
      case IB_LENS:
	while(k<(32)){
	  if(n!=0){
	    r=Z_OK;
	  }
	  else{
	    this.bitb=b; this.bitk=k; 
	    z.avail_in=n;
	    z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    this.write=q;
	    return this.inflate_flush(z,r);
	  };
	  n--;
	  b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}

	if ((((~b) >>> 16) & 0xffff) != (b & 0xffff)){
	  this.mode = BAD;
	  z.msg = "invalid stored block lengths";
	  r = Z_DATA_ERROR;

	  this.bitb=b; this.bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  this.write=q;
	  return this.inflate_flush(z,r);
	}
	this.left = (b & 0xffff);
	b = k = 0;                       // dump bits
	this.mode = this.left!=0 ? IB_STORED : (this.last!=0 ? IB_DRY : IB_TYPE);
	break;
      case IB_STORED:
	if (n == 0){
	  this.bitb=b; this.bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  write=q;
	  return this.inflate_flush(z,r);
	}

	if(m==0){
	  if(q==end&&read!=0){
	    q=0; m=(q<this.read ? this.read-q-1 : this.end-q);
	  }
	  if(m==0){
	    this.write=q; 
	    r=this.inflate_flush(z,r);
	    q=this.write; m = (q < this.read ? this.read-q-1 : this.end-q);
	    if(q==this.end && this.read != 0){
	      q=0; m = (q < this.read ? this.read-q-1 : this.end-q);
	    }
	    if(m==0){
	      this.bitb=b; this.bitk=k; 
	      z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      this.write=q;
	      return this.inflate_flush(z,r);
	    }
	  }
	}
	r=Z_OK;

	t = this.left;
	if(t>n) t = n;
	if(t>m) t = m;
	arrayCopy(z.next_in, p, window, q, t);
	p += t;  n -= t;
	q += t;  m -= t;
	if ((this.left -= t) != 0)
	  break;
	this.mode = (this.last != 0 ? IB_DRY : IB_TYPE);
	break;
      case IB_TABLE:

	while(k<(14)){
	  if(n!=0){
	    r=Z_OK;
	  }
	  else{
	    this.bitb=b; this.bitk=k; 
	    z.avail_in=n;
	    z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    this.write=q;
	    return this.inflate_flush(z,r);
	  };
	  n--;
	  b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}

	this.table = t = (b & 0x3fff);
	if ((t & 0x1f) > 29 || ((t >> 5) & 0x1f) > 29)
	  {
	    this.mode = IB_BAD;
	    z.msg = "too many length or distance symbols";
	    r = Z_DATA_ERROR;

	    this.bitb=b; this.bitk=k; 
	    z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    this.write=q;
	    return this.inflate_flush(z,r);
	  }
	t = 258 + (t & 0x1f) + ((t >> 5) & 0x1f);
	if(this.blens==null || this.blens.length<t){
	    this.blens=new Int32Array(t);
	}
	else{
	  for(var i=0; i<t; i++){
              this.blens[i]=0;
          }
	}

	{b>>>=(14);k-=(14);}

	this.index = 0;
	mode = IB_BTREE;
      case IB_BTREE:
	while (this.index < 4 + (this.table >>> 10)){
	  while(k<(3)){
	    if(n!=0){
	      r=Z_OK;
	    }
	    else{
	      this.bitb=b; this.bitk=k; 
	      z.avail_in=n;
	      z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      this.write=q;
	      return this.inflate_flush(z,r);
	    };
	    n--;
	    b|=(z.next_in[p++]&0xff)<<k;
	    k+=8;
	  }

	  this.blens[INFBLOCKS_BORDER[this.index++]] = b&7;

	  {b>>>=(3);k-=(3);}
	}

	while(this.index < 19){
	  this.blens[INFBLOCKS_BORDER[this.index++]] = 0;
	}

	this.bb[0] = 7;
	t = this.inftree.inflate_trees_bits(this.blens, this.bb, this.tb, this.hufts, z);
	if (t != Z_OK){
	  r = t;
	  if (r == Z_DATA_ERROR){
	    this.blens=null;
	    this.mode = IB_BAD;
	  }

	  this.bitb=b; this.bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  write=q;
	  return this.inflate_flush(z,r);
	}

	this.index = 0;
	this.mode = IB_DTREE;
      case IB_DTREE:
	while (true){
	  t = this.table;
	  if(!(this.index < 258 + (t & 0x1f) + ((t >> 5) & 0x1f))){
	    break;
	  }

	  var h; //int[]
	  var i, j, c;

	  t = this.bb[0];

	  while(k<(t)){
	    if(n!=0){
	      r=Z_OK;
	    }
	    else{
	      this.bitb=b; this.bitk=k; 
	      z.avail_in=n;
	      z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      this.write=q;
	      return this.inflate_flush(z,r);
	    };
	    n--;
	    b|=(z.next_in[p++]&0xff)<<k;
	    k+=8;
	  }

//	  if (this.tb[0]==-1){
//            dlog("null...");
//	  }

	  t=this.hufts[(this.tb[0]+(b & inflate_mask[t]))*3+1];
	  c=this.hufts[(this.tb[0]+(b & inflate_mask[t]))*3+2];

	  if (c < 16){
	    b>>>=(t);k-=(t);
	    this.blens[this.index++] = c;
	  }
	  else { // c == 16..18
	    i = c == 18 ? 7 : c - 14;
	    j = c == 18 ? 11 : 3;

	    while(k<(t+i)){
	      if(n!=0){
		r=Z_OK;
	      }
	      else{
		this.bitb=b; this.bitk=k; 
		z.avail_in=n;
		z.total_in+=p-z.next_in_index;z.next_in_index=p;
		this.write=q;
		return this.inflate_flush(z,r);
	      };
	      n--;
	      b|=(z.next_in[p++]&0xff)<<k;
	      k+=8;
	    }

	    b>>>=(t);k-=(t);

	    j += (b & inflate_mask[i]);

	    b>>>=(i);k-=(i);

	    i = this.index;
	    t = this.table;
	    if (i + j > 258 + (t & 0x1f) + ((t >> 5) & 0x1f) ||
		(c == 16 && i < 1)){
	      this.blens=null;
	      this.mode = IB_BAD;
	      z.msg = "invalid bit length repeat";
	      r = Z_DATA_ERROR;

	      this.bitb=b; this.bitk=k; 
	      z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      this.write=q;
	      return this.inflate_flush(z,r);
	    }

	    c = c == 16 ? this.blens[i-1] : 0;
	    do{
	      this.blens[i++] = c;
	    }
	    while (--j!=0);
	    this.index = i;
	  }
	}

	this.tb[0]=-1;
	{
	    var bl=new Int32Array(1);
	    var bd=new Int32Array(1);
	    var tl=new Int32Array(1);
	    var td=new Int32Array(1);
	    bl[0] = 9;         // must be <= 9 for lookahead assumptions
	    bd[0] = 6;         // must be <= 9 for lookahead assumptions

	    t = this.table;
	    t = this.inftree.inflate_trees_dynamic(257 + (t & 0x1f), 
					      1 + ((t >> 5) & 0x1f),
					      this.blens, bl, bd, tl, td, this.hufts, z);

	    if (t != Z_OK){
	        if (t == Z_DATA_ERROR){
	            this.blens=null;
	            this.mode = BAD;
	        }
	        r = t;

	        this.bitb=b; this.bitk=k; 
	        z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	        this.write=q;
	        return this.inflate_flush(z,r);
	    }
	    this.codes.init(bl[0], bd[0], this.hufts, tl[0], this.hufts, td[0], z);
	}
	this.mode = IB_CODES;
      case IB_CODES:
	this.bitb=b; this.bitk=k;
	z.avail_in=n; z.total_in+=p-z.next_in_index;z.next_in_index=p;
	this.write=q;

	if ((r = this.codes.proc(this, z, r)) != Z_STREAM_END){
	  return this.inflate_flush(z, r);
	}
	r = Z_OK;
	this.codes.free(z);

	p=z.next_in_index; n=z.avail_in;b=this.bitb;k=this.bitk;
	q=this.write;m = (q < this.read ? this.read-q-1 : this.end-q);

	if (this.last==0){
	  this.mode = IB_TYPE;
	  break;
	}
	this.mode = IB_DRY;
      case IB_DRY:
	this.write=q; 
	r = this.inflate_flush(z, r); 
	q=this.write; m = (q < this.read ? this.read-q-1 : this.end-q);
	if (this.read != this.write){
	  this.bitb=b; this.bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  this.write=q;
	  return this.inflate_flush(z, r);
	}
	mode = DONE;
      case IB_DONE:
	r = Z_STREAM_END;

	this.bitb=b; this.bitk=k; 
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	this.write=q;
	return this.inflate_flush(z, r);
      case IB_BAD:
	r = Z_DATA_ERROR;

	this.bitb=b; this.bitk=k; 
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	this.write=q;
	return this.inflate_flush(z, r);

      default:
	r = Z_STREAM_ERROR;

	this.bitb=b; this.bitk=k; 
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	this.write=q;
	return this.inflate_flush(z, r);
      }
    }
  }

InfBlocks.prototype.free = function(z){
    this.reset(z, null);
    this.window=null;
    this.hufts=null;
}

InfBlocks.prototype.set_dictionary = function(d, start, n){
    arrayCopy(d, start, window, 0, n);
    this.read = this.write = n;
}

  // Returns true if inflate is currently at the end of a block generated
  // by Z_SYNC_FLUSH or Z_FULL_FLUSH. 
InfBlocks.prototype.sync_point = function(){
    return this.mode == IB_LENS;
}

  // copy as much as possible from the sliding window to the output area
InfBlocks.prototype.inflate_flush = function(z, r){
    var n;
    var p;
    var q;

    // local copies of source and destination pointers
    p = z.next_out_index;
    q = this.read;

    // compute number of bytes to copy as far as end of window
    n = ((q <= this.write ? this.write : this.end) - q);
    if (n > z.avail_out) n = z.avail_out;
    if (n!=0 && r == Z_BUF_ERROR) r = Z_OK;

    // update counters
    z.avail_out -= n;
    z.total_out += n;

    // update check information
    if(this.checkfn != null)
      z.adler=this.check=z._adler.adler32(this.check, this.window, q, n);

    // copy as far as end of window
    arrayCopy(this.window, q, z.next_out, p, n);
    p += n;
    q += n;

    // see if more to copy at beginning of window
    if (q == this.end){
      // wrap pointers
      q = 0;
      if (this.write == this.end)
        this.write = 0;

      // compute bytes to copy
      n = this.write - q;
      if (n > z.avail_out) n = z.avail_out;
      if (n!=0 && r == Z_BUF_ERROR) r = Z_OK;

      // update counters
      z.avail_out -= n;
      z.total_out += n;

      // update check information
      if(this.checkfn != null)
	z.adler=this.check=z._adler.adler32(this.check, this.window, q, n);

      // copy
      arrayCopy(this.window, q, z.next_out, p, n);
      p += n;
      q += n;
    }

    // update pointers
    z.next_out_index = p;
    this.read = q;

    // done
    return r;
  }

//
// InfCodes.java
//

var IC_START=0;  // x: set up for LEN
var IC_LEN=1;    // i: get length/literal/eob next
var IC_LENEXT=2; // i: getting length extra (have base)
var IC_DIST=3;   // i: get distance next
var IC_DISTEXT=4;// i: getting distance extra
var IC_COPY=5;   // o: copying bytes in window, waiting for space
var IC_LIT=6;    // o: got literal, waiting for output space
var IC_WASH=7;   // o: got eob, possibly still output waiting
var IC_END=8;    // x: got eob and all data flushed
var IC_BADCODE=9;// x: got error

function InfCodes() {
}

InfCodes.prototype.init = function(bl, bd, tl, tl_index, td, td_index, z) {
    this.mode=IC_START;
    this.lbits=bl;
    this.dbits=bd;
    this.ltree=tl;
    this.ltree_index=tl_index;
    this.dtree = td;
    this.dtree_index=td_index;
    this.tree=null;
}

InfCodes.prototype.proc = function(s, z, r){ 
    var j;              // temporary storage
    var t;              // temporary pointer (int[])
    var tindex;         // temporary pointer
    var e;              // extra bits or operation
    var b=0;            // bit buffer
    var k=0;            // bits in bit buffer
    var p=0;            // input data pointer
    var n;              // bytes available there
    var q;              // output window write pointer
    var m;              // bytes to end of window or read pointer
    var f;              // pointer to copy strings from

    // copy input/output information to locals (UPDATE macro restores)
    p=z.next_in_index;n=z.avail_in;b=s.bitb;k=s.bitk;
    q=s.write;m=q<s.read?s.read-q-1:s.end-q;

    // process input and output based on current state
    while (true){
      switch (this.mode){
	// waiting for "i:"=input, "o:"=output, "x:"=nothing
      case IC_START:         // x: set up for LEN
	if (m >= 258 && n >= 10){

	  s.bitb=b;s.bitk=k;
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  s.write=q;
	  r = this.inflate_fast(this.lbits, this.dbits, 
			   this.ltree, this.ltree_index, 
			   this.dtree, this.dtree_index,
			   s, z);

	  p=z.next_in_index;n=z.avail_in;b=s.bitb;k=s.bitk;
	  q=s.write;m=q<s.read?s.read-q-1:s.end-q;

	  if (r != Z_OK){
	    this.mode = r == Z_STREAM_END ? IC_WASH : IC_BADCODE;
	    break;
	  }
	}
	this.need = this.lbits;
	this.tree = this.ltree;
	this.tree_index=this.ltree_index;

	this.mode = IC_LEN;
      case IC_LEN:           // i: get length/literal/eob next
	j = this.need;

	while(k<(j)){
	  if(n!=0)r=Z_OK;
	  else{

	    s.bitb=b;s.bitk=k;
	    z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    s.write=q;
	    return s.inflate_flush(z,r);
	  }
	  n--;
	  b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}

	tindex=(this.tree_index+(b&inflate_mask[j]))*3;

	b>>>=(this.tree[tindex+1]);
	k-=(this.tree[tindex+1]);

	e=this.tree[tindex];

	if(e == 0){               // literal
	  this.lit = this.tree[tindex+2];
	  this.mode = IC_LIT;
	  break;
	}
	if((e & 16)!=0 ){          // length
	  this.get = e & 15;
	  this.len = this.tree[tindex+2];
	  this.mode = IC_LENEXT;
	  break;
	}
	if ((e & 64) == 0){        // next table
	  this.need = e;
	  this.tree_index = tindex/3 + this.tree[tindex+2];
	  break;
	}
	if ((e & 32)!=0){               // end of block
	  this.mode = IC_WASH;
	  break;
	}
	this.mode = IC_BADCODE;        // invalid code
	z.msg = "invalid literal/length code";
	r = Z_DATA_ERROR;

	s.bitb=b;s.bitk=k;
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	s.write=q;
	return s.inflate_flush(z,r);

      case IC_LENEXT:        // i: getting length extra (have base)
	j = this.get;

	while(k<(j)){
	  if(n!=0)r=Z_OK;
	  else{

	    s.bitb=b;s.bitk=k;
	    z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    s.write=q;
	    return s.inflate_flush(z,r);
	  }
	  n--; b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}

	this.len += (b & inflate_mask[j]);

	b>>=j;
	k-=j;

	this.need = this.dbits;
	this.tree = this.dtree;
	this.tree_index = this.dtree_index;
	this.mode = IC_DIST;
      case IC_DIST:          // i: get distance next
	j = this.need;

	while(k<(j)){
	  if(n!=0)r=Z_OK;
	  else{

	    s.bitb=b;s.bitk=k;
	    z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    s.write=q;
	    return s.inflate_flush(z,r);
	  }
	  n--; b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}

	tindex=(this.tree_index+(b & inflate_mask[j]))*3;

	b>>=this.tree[tindex+1];
	k-=this.tree[tindex+1];

	e = (this.tree[tindex]);
	if((e & 16)!=0){               // distance
	  this.get = e & 15;
	  this.dist = this.tree[tindex+2];
	  this.mode = IC_DISTEXT;
	  break;
	}
	if ((e & 64) == 0){        // next table
	  this.need = e;
	  this.tree_index = tindex/3 + this.tree[tindex+2];
	  break;
	}
	this.mode = IC_BADCODE;        // invalid code
	z.msg = "invalid distance code";
	r = Z_DATA_ERROR;

	s.bitb=b;s.bitk=k;
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	s.write=q;
	return s.inflate_flush(z,r);

      case IC_DISTEXT:       // i: getting distance extra
	j = this.get;

	while(k<(j)){
	  if(n!=0)r=Z_OK;
	  else{

	    s.bitb=b;s.bitk=k;
	    z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    s.write=q;
	    return s.inflate_flush(z,r);
	  }
	  n--; b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}

	this.dist += (b & inflate_mask[j]);

	b>>=j;
	k-=j;

	this.mode = IC_COPY;
      case IC_COPY:          // o: copying bytes in window, waiting for space
        f = q - this.dist;
        while(f < 0){     // modulo window size-"while" instead
          f += s.end;     // of "if" handles invalid distances
	}
	while (this.len!=0){

	  if(m==0){
	    if(q==s.end&&s.read!=0){q=0;m=q<s.read?s.read-q-1:s.end-q;}
	    if(m==0){
	      s.write=q; r=s.inflate_flush(z,r);
	      q=s.write;m=q<s.read?s.read-q-1:s.end-q;

	      if(q==s.end&&s.read!=0){q=0;m=q<s.read?s.read-q-1:s.end-q;}

	      if(m==0){
		s.bitb=b;s.bitk=k;
		z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
		s.write=q;
		return s.inflate_flush(z,r);
	      }  
	    }
	  }

	  s.window[q++]=s.window[f++]; m--;

	  if (f == s.end)
            f = 0;
	  this.len--;
	}
	this.mode = IC_START;
	break;
      case IC_LIT:           // o: got literal, waiting for output space
	if(m==0){
	  if(q==s.end&&s.read!=0){q=0;m=q<s.read?s.read-q-1:s.end-q;}
	  if(m==0){
	    s.write=q; r=s.inflate_flush(z,r);
	    q=s.write;m=q<s.read?s.read-q-1:s.end-q;

	    if(q==s.end&&s.read!=0){q=0;m=q<s.read?s.read-q-1:s.end-q;}
	    if(m==0){
	      s.bitb=b;s.bitk=k;
	      z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      s.write=q;
	      return s.inflate_flush(z,r);
	    }
	  }
	}
	r=Z_OK;

	s.window[q++]=this.lit; m--;

	this.mode = IC_START;
	break;
      case IC_WASH:           // o: got eob, possibly more output
	if (k > 7){        // return unused byte, if any
	  k -= 8;
	  n++;
	  p--;             // can always return one
	}

	s.write=q; r=s.inflate_flush(z,r);
	q=s.write;m=q<s.read?s.read-q-1:s.end-q;

	if (s.read != s.write){
	  s.bitb=b;s.bitk=k;
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  s.write=q;
	  return s.inflate_flush(z,r);
	}
	this.mode = IC_END;
      case IC_END:
	r = Z_STREAM_END;
	s.bitb=b;s.bitk=k;
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	s.write=q;
	return s.inflate_flush(z,r);

      case IC_BADCODE:       // x: got error

	r = Z_DATA_ERROR;

	s.bitb=b;s.bitk=k;
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	s.write=q;
	return s.inflate_flush(z,r);

      default:
	r = Z_STREAM_ERROR;

	s.bitb=b;s.bitk=k;
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	s.write=q;
	return s.inflate_flush(z,r);
      }
    }
  }

InfCodes.prototype.free = function(z){
    //  ZFREE(z, c);
}

  // Called with number of bytes left to write in window at least 258
  // (the maximum string length) and number of input bytes available
  // at least ten.  The ten bytes are six bytes for the longest length/
  // distance pair plus four bytes for overloading the bit buffer.

InfCodes.prototype.inflate_fast = function(bl, bd, tl, tl_index, td, td_index, s, z) {
    var t;                // temporary pointer
    var   tp;             // temporary pointer (int[])
    var tp_index;         // temporary pointer
    var e;                // extra bits or operation
    var b;                // bit buffer
    var k;                // bits in bit buffer
    var p;                // input data pointer
    var n;                // bytes available there
    var q;                // output window write pointer
    var m;                // bytes to end of window or read pointer
    var ml;               // mask for literal/length tree
    var md;               // mask for distance tree
    var c;                // bytes to copy
    var d;                // distance back to copy from
    var r;                // copy source pointer

    var tp_index_t_3;     // (tp_index+t)*3

    // load input, output, bit values
    p=z.next_in_index;n=z.avail_in;b=s.bitb;k=s.bitk;
    q=s.write;m=q<s.read?s.read-q-1:s.end-q;

    // initialize masks
    ml = inflate_mask[bl];
    md = inflate_mask[bd];

    // do until not enough input or output space for fast loop
    do {                          // assume called with m >= 258 && n >= 10
      // get literal/length code
      while(k<(20)){              // max bits for literal/length code
	n--;
	b|=(z.next_in[p++]&0xff)<<k;k+=8;
      }

      t= b&ml;
      tp=tl; 
      tp_index=tl_index;
      tp_index_t_3=(tp_index+t)*3;
      if ((e = tp[tp_index_t_3]) == 0){
	b>>=(tp[tp_index_t_3+1]); k-=(tp[tp_index_t_3+1]);

	s.window[q++] = tp[tp_index_t_3+2];
	m--;
	continue;
      }
      do {

	b>>=(tp[tp_index_t_3+1]); k-=(tp[tp_index_t_3+1]);

	if((e&16)!=0){
	  e &= 15;
	  c = tp[tp_index_t_3+2] + (b & inflate_mask[e]);

	  b>>=e; k-=e;

	  // decode distance base of block to copy
	  while(k<(15)){           // max bits for distance code
	    n--;
	    b|=(z.next_in[p++]&0xff)<<k;k+=8;
	  }

	  t= b&md;
	  tp=td;
	  tp_index=td_index;
          tp_index_t_3=(tp_index+t)*3;
	  e = tp[tp_index_t_3];

	  do {

	    b>>=(tp[tp_index_t_3+1]); k-=(tp[tp_index_t_3+1]);

	    if((e&16)!=0){
	      // get extra bits to add to distance base
	      e &= 15;
	      while(k<(e)){         // get extra bits (up to 13)
		n--;
		b|=(z.next_in[p++]&0xff)<<k;k+=8;
	      }

	      d = tp[tp_index_t_3+2] + (b&inflate_mask[e]);

	      b>>=(e); k-=(e);

	      // do the copy
	      m -= c;
	      if (q >= d){                // offset before dest
		//  just copy
		r=q-d;
		if(q-r>0 && 2>(q-r)){           
		  s.window[q++]=s.window[r++]; // minimum count is three,
		  s.window[q++]=s.window[r++]; // so unroll loop a little
		  c-=2;
		}
		else{
		  s.window[q++]=s.window[r++]; // minimum count is three,
		  s.window[q++]=s.window[r++]; // so unroll loop a little
		  c-=2;
		}
	      }
	      else{                  // else offset after destination
                r=q-d;
                do{
                  r+=s.end;          // force pointer in window
                }while(r<0);         // covers invalid distances
		e=s.end-r;
		if(c>e){             // if source crosses,
		  c-=e;              // wrapped copy
		  if(q-r>0 && e>(q-r)){           
		    do{s.window[q++] = s.window[r++];}
		    while(--e!=0);
		  }
		  else{
		    arrayCopy(s.window, r, s.window, q, e);
		    q+=e; r+=e; e=0;
		  }
		  r = 0;                  // copy rest from start of window
		}

	      }

	      // copy all or what's left
              do{s.window[q++] = s.window[r++];}
		while(--c!=0);
	      break;
	    }
	    else if((e&64)==0){
	      t+=tp[tp_index_t_3+2];
	      t+=(b&inflate_mask[e]);
	      tp_index_t_3=(tp_index+t)*3;
	      e=tp[tp_index_t_3];
	    }
	    else{
	      z.msg = "invalid distance code";

	      c=z.avail_in-n;c=(k>>3)<c?k>>3:c;n+=c;p-=c;k-=c<<3;

	      s.bitb=b;s.bitk=k;
	      z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      s.write=q;

	      return Z_DATA_ERROR;
	    }
	  }
	  while(true);
	  break;
	}

	if((e&64)==0){
	  t+=tp[tp_index_t_3+2];
	  t+=(b&inflate_mask[e]);
	  tp_index_t_3=(tp_index+t)*3;
	  if((e=tp[tp_index_t_3])==0){

	    b>>=(tp[tp_index_t_3+1]); k-=(tp[tp_index_t_3+1]);

	    s.window[q++]=tp[tp_index_t_3+2];
	    m--;
	    break;
	  }
	}
	else if((e&32)!=0){

	  c=z.avail_in-n;c=(k>>3)<c?k>>3:c;n+=c;p-=c;k-=c<<3;
 
	  s.bitb=b;s.bitk=k;
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  s.write=q;

	  return Z_STREAM_END;
	}
	else{
	  z.msg="invalid literal/length code";

	  c=z.avail_in-n;c=(k>>3)<c?k>>3:c;n+=c;p-=c;k-=c<<3;

	  s.bitb=b;s.bitk=k;
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  s.write=q;

	  return Z_DATA_ERROR;
	}
      } 
      while(true);
    } 
    while(m>=258 && n>= 10);

    // not enough input or output--restore pointers and return
    c=z.avail_in-n;c=(k>>3)<c?k>>3:c;n+=c;p-=c;k-=c<<3;

    s.bitb=b;s.bitk=k;
    z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
    s.write=q;

    return Z_OK;
}

//
// InfTree.java
//

function InfTree() {
}

InfTree.prototype.huft_build = function(b, bindex, n, s, d, e, t, m, hp, hn, v) {

    // Given a list of code lengths and a maximum table size, make a set of
    // tables to decode that set of codes.  Return Z_OK on success, Z_BUF_ERROR
    // if the given code set is incomplete (the tables are still built in this
    // case), Z_DATA_ERROR if the input is invalid (an over-subscribed set of
    // lengths), or Z_MEM_ERROR if not enough memory.

    var a;                       // counter for codes of length k
    var f;                       // i repeats in table every f entries
    var g;                       // maximum code length
    var h;                       // table level
    var i;                       // counter, current code
    var j;                       // counter
    var k;                       // number of bits in current code
    var l;                       // bits per table (returned in m)
    var mask;                    // (1 << w) - 1, to avoid cc -O bug on HP
    var p;                       // pointer into c[], b[], or v[]
    var q;                       // points to current table
    var w;                       // bits before this table == (l * h)
    var xp;                      // pointer into x
    var y;                       // number of dummy codes added
    var z;                       // number of entries in current table

    // Generate counts for each bit length

    p = 0; i = n;
    do {
      this.c[b[bindex+p]]++; p++; i--;   // assume all entries <= BMAX
    }while(i!=0);

    if(this.c[0] == n){                // null input--all zero length codes
      t[0] = -1;
      m[0] = 0;
      return Z_OK;
    }

    // Find minimum and maximum length, bound *m by those
    l = m[0];
    for (j = 1; j <= BMAX; j++)
      if(this.c[j]!=0) break;
    k = j;                        // minimum code length
    if(l < j){
      l = j;
    }
    for (i = BMAX; i!=0; i--){
      if(this.c[i]!=0) break;
    }
    g = i;                        // maximum code length
    if(l > i){
      l = i;
    }
    m[0] = l;

    // Adjust last length count to fill out codes, if needed
    for (y = 1 << j; j < i; j++, y <<= 1){
      if ((y -= this.c[j]) < 0){
        return Z_DATA_ERROR;
      }
    }
    if ((y -= this.c[i]) < 0){
      return Z_DATA_ERROR;
    }
    this.c[i] += y;

    // Generate starting offsets into the value table for each length
    this.x[1] = j = 0;
    p = 1;  xp = 2;
    while (--i!=0) {                 // note that i == g from above
      this.x[xp] = (j += this.c[p]);
      xp++;
      p++;
    }

    // Make a table of values in order of bit lengths
    i = 0; p = 0;
    do {
      if ((j = b[bindex+p]) != 0){
        this.v[this.x[j]++] = i;
      }
      p++;
    }
    while (++i < n);
    n = this.x[g];                     // set n to length of v

    // Generate the Huffman codes and for each, make the table entries
    this.x[0] = i = 0;                 // first Huffman code is zero
    p = 0;                        // grab values in bit order
    h = -1;                       // no tables yet--level -1
    w = -l;                       // bits decoded == (l * h)
    this.u[0] = 0;                     // just to keep compilers happy
    q = 0;                        // ditto
    z = 0;                        // ditto

    // go through the bit lengths (k already is bits in shortest code)
    for (; k <= g; k++){
      a = this.c[k];
      while (a--!=0){
	// here i is the Huffman code of length k bits for value *p
	// make tables up to required level
        while (k > w + l){
          h++;
          w += l;                 // previous table always l bits
	  // compute minimum size table less than or equal to l bits
          z = g - w;
          z = (z > l) ? l : z;        // table size upper limit
          if((f=1<<(j=k-w))>a+1){     // try a k-w bit table
                                      // too few codes for k-w bit table
            f -= a + 1;               // deduct codes from patterns left
            xp = k;
            if(j < z){
              while (++j < z){        // try smaller tables up to z bits
                if((f <<= 1) <= this.c[++xp])
                  break;              // enough codes to use up j bits
                f -= this.c[xp];           // else deduct codes from patterns
              }
	    }
          }
          z = 1 << j;                 // table entries for j-bit table

	  // allocate new table
          if (this.hn[0] + z > MANY){       // (note: doesn't matter for fixed)
            return Z_DATA_ERROR;       // overflow of MANY
          }
          this.u[h] = q = /*hp+*/ this.hn[0];   // DEBUG
          this.hn[0] += z;
 
	  // connect to last table, if there is one
	  if(h!=0){
            this.x[h]=i;           // save pattern for backing up
            this.r[0]=j;     // bits in this table
            this.r[1]=l;     // bits to dump before this table
            j=i>>>(w - l);
            this.r[2] = (q - this.u[h-1] - j);               // offset to this table
            arrayCopy(this.r, 0, hp, (this.u[h-1]+j)*3, 3); // connect to last table
          }
          else{
            t[0] = q;               // first table is returned result
	  }
        }

	// set up table entry in r
        this.r[1] = (k - w);
        if (p >= n){
          this.r[0] = 128 + 64;      // out of values--invalid code
	}
        else if (v[p] < s){
          this.r[0] = (this.v[p] < 256 ? 0 : 32 + 64);  // 256 is end-of-block
          this.r[2] = this.v[p++];          // simple code is just the value
        }
        else{
          this.r[0]=(e[this.v[p]-s]+16+64); // non-simple--look up in lists
          this.r[2]=d[this.v[p++] - s];
        }

        // fill code-like entries with r
        f=1<<(k-w);
        for (j=i>>>w;j<z;j+=f){
          arrayCopy(this.r, 0, hp, (q+j)*3, 3);
	}

	// backwards increment the k-bit code i
        for (j = 1 << (k - 1); (i & j)!=0; j >>>= 1){
          i ^= j;
	}
        i ^= j;

	// backup over finished tables
        mask = (1 << w) - 1;      // needed on HP, cc -O bug
        while ((i & mask) != this.x[h]){
          h--;                    // don't need to update q
          w -= l;
          mask = (1 << w) - 1;
        }
      }
    }
    // Return Z_BUF_ERROR if we were given an incomplete table
    return y != 0 && g != 1 ? Z_BUF_ERROR : Z_OK;
}

InfTree.prototype.inflate_trees_bits = function(c, bb, tb, hp, z) {
    var result;
    this.initWorkArea(19);
    this.hn[0]=0;
    result = this.huft_build(c, 0, 19, 19, null, null, tb, bb, hp, this.hn, this.v);

    if(result == Z_DATA_ERROR){
      z.msg = "oversubscribed dynamic bit lengths tree";
    }
    else if(result == Z_BUF_ERROR || bb[0] == 0){
      z.msg = "incomplete dynamic bit lengths tree";
      result = Z_DATA_ERROR;
    }
    return result;
}

InfTree.prototype.inflate_trees_dynamic = function(nl, nd, c, bl, bd, tl, td, hp, z) {
    var result;

    // build literal/length tree
    this.initWorkArea(288);
    this.hn[0]=0;
    result = this.huft_build(c, 0, nl, 257, cplens, cplext, tl, bl, hp, this.hn, this.v);
    if (result != Z_OK || bl[0] == 0){
      if(result == Z_DATA_ERROR){
        z.msg = "oversubscribed literal/length tree";
      }
      else if (result != Z_MEM_ERROR){
        z.msg = "incomplete literal/length tree";
        result = Z_DATA_ERROR;
      }
      return result;
    }

    // build distance tree
    this.initWorkArea(288);
    result = this.huft_build(c, nl, nd, 0, cpdist, cpdext, td, bd, hp, this.hn, this.v);

    if (result != Z_OK || (bd[0] == 0 && nl > 257)){
      if (result == Z_DATA_ERROR){
        z.msg = "oversubscribed distance tree";
      }
      else if (result == Z_BUF_ERROR) {
        z.msg = "incomplete distance tree";
        result = Z_DATA_ERROR;
      }
      else if (result != Z_MEM_ERROR){
        z.msg = "empty distance tree with lengths";
        result = Z_DATA_ERROR;
      }
      return result;
    }

    return Z_OK;
}
/*
  static int inflate_trees_fixed(int[] bl,  //literal desired/actual bit depth
                                 int[] bd,  //distance desired/actual bit depth
                                 int[][] tl,//literal/length tree result
                                 int[][] td,//distance tree result 
                                 ZStream z  //for memory allocation
				 ){

*/

function inflate_trees_fixed(bl, bd, tl, td, z) {
    bl[0]=fixed_bl;
    bd[0]=fixed_bd;
    tl[0]=fixed_tl;
    td[0]=fixed_td;
    return Z_OK;
}

InfTree.prototype.initWorkArea = function(vsize){
    if(this.hn==null){
        this.hn=new Int32Array(1);
        this.v=new Int32Array(vsize);
        this.c=new Int32Array(BMAX+1);
        this.r=new Int32Array(3);
        this.u=new Int32Array(BMAX);
        this.x=new Int32Array(BMAX+1);
    }
    if(this.v.length<vsize){ 
        this.v=new Int32Array(vsize); 
    }
    for(var i=0; i<vsize; i++){this.v[i]=0;}
    for(var i=0; i<BMAX+1; i++){this.c[i]=0;}
    for(var i=0; i<3; i++){this.r[i]=0;}
//  for(int i=0; i<BMAX; i++){u[i]=0;}
    arrayCopy(this.c, 0, this.u, 0, BMAX);
//  for(int i=0; i<BMAX+1; i++){x[i]=0;}
    arrayCopy(this.c, 0, this.x, 0, BMAX+1);
}

var testArray = new Uint8Array(1);
var hasSubarray = (typeof testArray.subarray === 'function');
var hasSlice = false; /* (typeof testArray.slice === 'function'); */ // Chrome slice performance is so dire that we're currently not using it...

function arrayCopy(src, srcOffset, dest, destOffset, count) {
    if (count == 0) {
        return;
    } 
    if (!src) {
        throw "Undef src";
    } else if (!dest) {
        throw "Undef dest";
    }

    if (srcOffset == 0 && count == src.length) {
        arrayCopy_fast(src, dest, destOffset);
    } else if (hasSubarray) {
        arrayCopy_fast(src.subarray(srcOffset, srcOffset + count), dest, destOffset); 
    } else if (src.BYTES_PER_ELEMENT == 1 && count > 100) {
        arrayCopy_fast(new Uint8Array(src.buffer, src.byteOffset + srcOffset, count), dest, destOffset);
    } else { 
        arrayCopy_slow(src, srcOffset, dest, destOffset, count);
    }

}

function arrayCopy_slow(src, srcOffset, dest, destOffset, count) {

    // dlog('_slow call: srcOffset=' + srcOffset + '; destOffset=' + destOffset + '; count=' + count);

     for (var i = 0; i < count; ++i) {
        dest[destOffset + i] = src[srcOffset + i];
    }
}

function arrayCopy_fast(src, dest, destOffset) {
    dest.set(src, destOffset);
}


  // largest prime smaller than 65536
var ADLER_BASE=65521; 
  // NMAX is the largest n such that 255n(n+1)/2 + (n+1)(BASE-1) <= 2^32-1
var ADLER_NMAX=5552;

function adler32(adler, /* byte[] */ buf,  index, len){
    if(buf == null){ return 1; }

    var s1=adler&0xffff;
    var s2=(adler>>16)&0xffff;
    var k;

    while(len > 0) {
      k=len<ADLER_NMAX?len:ADLER_NMAX;
      len-=k;
      while(k>=16){
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        k-=16;
      }
      if(k!=0){
        do{
          s1+=buf[index++]&0xff; s2+=s1;
        }
        while(--k!=0);
      }
      s1%=ADLER_BASE;
      s2%=ADLER_BASE;
    }
    return (s2<<16)|s1;
}



function jszlib_inflate_buffer(buffer, start, length, afterUncOffset) {
    if (!start) {
        buffer = new Uint8Array(buffer);
    } else {
        buffer = new Uint8Array(buffer, start, length);
    }

    var z = new ZStream();
    z.inflateInit(DEF_WBITS, true);
    z.next_in = buffer;
    z.next_in_index = 0;
    z.avail_in = buffer.length;

    var oBlockList = [];
    var totalSize = 0;
    while (true) {
        var obuf = new Uint8Array(32000);
        z.next_out = obuf;
        z.next_out_index = 0;
        z.avail_out = obuf.length;
        var status = z.inflate(Z_NO_FLUSH);
        if (status != Z_OK && status != Z_STREAM_END && status != Z_BUF_ERROR) {
            throw z.msg;
        }
        if (z.avail_out != 0) {
            var newob = new Uint8Array(obuf.length - z.avail_out);
            arrayCopy(obuf, 0, newob, 0, (obuf.length - z.avail_out));
            obuf = newob;
        }
        oBlockList.push(obuf);
        totalSize += obuf.length;
        if (status == Z_STREAM_END || status == Z_BUF_ERROR) {
            break;
        }
    }

    if (afterUncOffset) {
        afterUncOffset[0] = (start || 0) + z.next_in_index;
    }

    if (oBlockList.length == 1) {
        return oBlockList[0].buffer;
    } else {
        var out = new Uint8Array(totalSize);
        var cursor = 0;
        for (var i = 0; i < oBlockList.length; ++i) {
            var b = oBlockList[i];
            arrayCopy(b, 0, out, cursor, b.length);
            cursor += b.length;
        }
        return out.buffer;
    }
}

if (typeof(module) !== 'undefined') {
  module.exports = {
    inflateBuffer: jszlib_inflate_buffer,
    arrayCopy: arrayCopy
  };
}
},{}]},{},[6])
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiZ2VuZXJhdGVkLmpzIiwic291cmNlcyI6WyIvVXNlcnMvcm51Z3JhaGEvRG9jdW1lbnRzL1dvcmtzcGFjZS9kYWxsaWFuY2UtcGx1Z2luL3dlYi1hcHAvbm9kZV9tb2R1bGVzL2d1bHAtYnJvd3NlcmlmeS9ub2RlX21vZHVsZXMvYnJvd3NlcmlmeS9ub2RlX21vZHVsZXMvYnJvd3Nlci1wYWNrL19wcmVsdWRlLmpzIiwiL1VzZXJzL3JudWdyYWhhL0RvY3VtZW50cy9Xb3Jrc3BhY2UvZGFsbGlhbmNlLXBsdWdpbi93ZWItYXBwL2pzL2JhbS5qcyIsIi9Vc2Vycy9ybnVncmFoYS9Eb2N1bWVudHMvV29ya3NwYWNlL2RhbGxpYW5jZS1wbHVnaW4vd2ViLWFwcC9qcy9iaWd3aWcuanMiLCIvVXNlcnMvcm51Z3JhaGEvRG9jdW1lbnRzL1dvcmtzcGFjZS9kYWxsaWFuY2UtcGx1Z2luL3dlYi1hcHAvanMvYmluLmpzIiwiL1VzZXJzL3JudWdyYWhhL0RvY3VtZW50cy9Xb3Jrc3BhY2UvZGFsbGlhbmNlLXBsdWdpbi93ZWItYXBwL2pzL2NvbG9yLmpzIiwiL1VzZXJzL3JudWdyYWhhL0RvY3VtZW50cy9Xb3Jrc3BhY2UvZGFsbGlhbmNlLXBsdWdpbi93ZWItYXBwL2pzL2Rhcy5qcyIsIi9Vc2Vycy9ybnVncmFoYS9Eb2N1bWVudHMvV29ya3NwYWNlL2RhbGxpYW5jZS1wbHVnaW4vd2ViLWFwcC9qcy9mYWtlX2QzZjEzMzE3LmpzIiwiL1VzZXJzL3JudWdyYWhhL0RvY3VtZW50cy9Xb3Jrc3BhY2UvZGFsbGlhbmNlLXBsdWdpbi93ZWItYXBwL2pzL2xoM3V0aWxzLmpzIiwiL1VzZXJzL3JudWdyYWhhL0RvY3VtZW50cy9Xb3Jrc3BhY2UvZGFsbGlhbmNlLXBsdWdpbi93ZWItYXBwL2pzL3NoYTEuanMiLCIvVXNlcnMvcm51Z3JhaGEvRG9jdW1lbnRzL1dvcmtzcGFjZS9kYWxsaWFuY2UtcGx1Z2luL3dlYi1hcHAvanMvc3BhbnMuanMiLCIvVXNlcnMvcm51Z3JhaGEvRG9jdW1lbnRzL1dvcmtzcGFjZS9kYWxsaWFuY2UtcGx1Z2luL3dlYi1hcHAvanMvdXRpbHMuanMiLCIvVXNlcnMvcm51Z3JhaGEvRG9jdW1lbnRzL1dvcmtzcGFjZS9kYWxsaWFuY2UtcGx1Z2luL3dlYi1hcHAvbm9kZV9tb2R1bGVzL2pzemxpYi9qcy9pbmZsYXRlLmpzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiJBQUFBO0FDQUE7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTs7QUN6Y0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7O0FDbmlDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBOztBQzVRQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBOztBQ3pIQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7O0FDcDBCQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTs7QUM3TEE7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7O0FDNUdBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBOztBQ25WQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBOztBQ3pOQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTs7QUMvWUE7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBIiwic291cmNlc0NvbnRlbnQiOlsiKGZ1bmN0aW9uIGUodCxuLHIpe2Z1bmN0aW9uIHMobyx1KXtpZighbltvXSl7aWYoIXRbb10pe3ZhciBhPXR5cGVvZiByZXF1aXJlPT1cImZ1bmN0aW9uXCImJnJlcXVpcmU7aWYoIXUmJmEpcmV0dXJuIGEobywhMCk7aWYoaSlyZXR1cm4gaShvLCEwKTt0aHJvdyBuZXcgRXJyb3IoXCJDYW5ub3QgZmluZCBtb2R1bGUgJ1wiK28rXCInXCIpfXZhciBmPW5bb109e2V4cG9ydHM6e319O3Rbb11bMF0uY2FsbChmLmV4cG9ydHMsZnVuY3Rpb24oZSl7dmFyIG49dFtvXVsxXVtlXTtyZXR1cm4gcyhuP246ZSl9LGYsZi5leHBvcnRzLGUsdCxuLHIpfXJldHVybiBuW29dLmV4cG9ydHN9dmFyIGk9dHlwZW9mIHJlcXVpcmU9PVwiZnVuY3Rpb25cIiYmcmVxdWlyZTtmb3IodmFyIG89MDtvPHIubGVuZ3RoO28rKylzKHJbb10pO3JldHVybiBzfSkiLCIvKiAtKi0gbW9kZTogamF2YXNjcmlwdDsgYy1iYXNpYy1vZmZzZXQ6IDQ7IGluZGVudC10YWJzLW1vZGU6IG5pbCAtKi0gKi9cblxuLy8gXG4vLyBEYWxsaWFuY2UgR2Vub21lIEV4cGxvcmVyXG4vLyAoYykgVGhvbWFzIERvd24gMjAwNi0yMDExXG4vL1xuLy8gYmFtLmpzOiBpbmRleGVkIGJpbmFyeSBhbGlnbm1lbnRzXG4vL1xuXG5cInVzZSBzdHJpY3RcIjtcblxuaWYgKHR5cGVvZihyZXF1aXJlKSAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICB2YXIgc3BhbnMgPSByZXF1aXJlKCcuL3NwYW5zJyk7XG4gICAgdmFyIFJhbmdlID0gc3BhbnMuUmFuZ2U7XG4gICAgdmFyIHVuaW9uID0gc3BhbnMudW5pb247XG4gICAgdmFyIGludGVyc2VjdGlvbiA9IHNwYW5zLmludGVyc2VjdGlvbjtcblxuICAgIHZhciBiaW4gPSByZXF1aXJlKCcuL2JpbicpO1xuICAgIHZhciByZWFkSW50ID0gYmluLnJlYWRJbnQ7XG4gICAgdmFyIHJlYWRTaG9ydCA9IGJpbi5yZWFkU2hvcnQ7XG4gICAgdmFyIHJlYWRCeXRlID0gYmluLnJlYWRCeXRlO1xuICAgIHZhciByZWFkSW50NjQgPSBiaW4ucmVhZEludDY0O1xuICAgIHZhciByZWFkRmxvYXQgPSBiaW4ucmVhZEZsb2F0O1xuXG4gICAgdmFyIGxoM3V0aWxzID0gcmVxdWlyZSgnLi9saDN1dGlscycpO1xuICAgIHZhciByZWFkVm9iID0gbGgzdXRpbHMucmVhZFZvYjtcbiAgICB2YXIgdW5iZ3pmID0gbGgzdXRpbHMudW5iZ3pmO1xuICAgIHZhciByZWcyYmlucyA9IGxoM3V0aWxzLnJlZzJiaW5zO1xuICAgIHZhciBDaHVuayA9IGxoM3V0aWxzLkNodW5rO1xufVxuXG5cbnZhciBCQU1fTUFHSUMgPSAweDE0ZDQxNDI7XG52YXIgQkFJX01BR0lDID0gMHgxNDk0MTQyO1xuXG52YXIgQmFtRmxhZ3MgPSB7XG4gICAgTVVMVElQTEVfU0VHTUVOVFM6ICAgICAgIDB4MSxcbiAgICBBTExfU0VHTUVOVFNfQUxJR046ICAgICAgMHgyLFxuICAgIFNFR01FTlRfVU5NQVBQRUQ6ICAgICAgICAweDQsXG4gICAgTkVYVF9TRUdNRU5UX1VOTUFQUEVEOiAgIDB4OCxcbiAgICBSRVZFUlNFX0NPTVBMRU1FTlQ6ICAgICAgMHgxMCxcbiAgICBORVhUX1JFVkVSU0VfQ09NUExFTUVOVDogMHgyMCxcbiAgICBGSVJTVF9TRUdNRU5UOiAgICAgICAgICAgMHg0MCxcbiAgICBMQVNUX1NFR01FTlQ6ICAgICAgICAgICAgMHg4MCxcbiAgICBTRUNPTkRBUllfQUxJR05NRU5UOiAgICAgMHgxMDAsXG4gICAgUUNfRkFJTDogICAgICAgICAgICAgICAgIDB4MjAwLFxuICAgIERVUExJQ0FURTogICAgICAgICAgICAgICAweDQwMCxcbiAgICBTVVBQTEVNRU5UQVJZOiAgICAgICAgICAgMHg4MDBcbn07XG5cbmZ1bmN0aW9uIEJhbUZpbGUoKSB7XG59XG5cbmZ1bmN0aW9uIG1ha2VCYW0oZGF0YSwgYmFpLCBjYWxsYmFjaykge1xuICAgIHZhciBiYW0gPSBuZXcgQmFtRmlsZSgpO1xuICAgIGJhbS5kYXRhID0gZGF0YTtcbiAgICBiYW0uYmFpID0gYmFpO1xuXG4gICAgYmFtLmJhaS5mZXRjaChmdW5jdGlvbihoZWFkZXIpIHsgICAvLyBEbyB3ZSByZWFsbHkgbmVlZCB0byBmZXRjaCB0aGUgd2hvbGUgdGhpbmc/IDotKFxuICAgICAgICBpZiAoIWhlYWRlcikge1xuICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKG51bGwsIFwiQ291bGRuJ3QgYWNjZXNzIEJBSVwiKTtcbiAgICAgICAgfVxuXG4gICAgICAgIHZhciB1bmNiYSA9IG5ldyBVaW50OEFycmF5KGhlYWRlcik7XG4gICAgICAgIHZhciBiYWlNYWdpYyA9IHJlYWRJbnQodW5jYmEsIDApO1xuICAgICAgICBpZiAoYmFpTWFnaWMgIT0gQkFJX01BR0lDKSB7XG4gICAgICAgICAgICByZXR1cm4gY2FsbGJhY2sobnVsbCwgJ05vdCBhIEJBSSBmaWxlLCBtYWdpYz0weCcgKyBiYWlNYWdpYy50b1N0cmluZygxNikpO1xuICAgICAgICB9XG5cbiAgICAgICAgdmFyIG5yZWYgPSByZWFkSW50KHVuY2JhLCA0KTtcblxuICAgICAgICBiYW0uaW5kaWNlcyA9IFtdO1xuXG4gICAgICAgIHZhciBwID0gODtcbiAgICAgICAgdmFyIG1pbkJsb2NrSW5kZXggPSAxMDAwMDAwMDAwO1xuICAgICAgICBmb3IgKHZhciByZWYgPSAwOyByZWYgPCBucmVmOyArK3JlZikge1xuICAgICAgICAgICAgdmFyIGJsb2NrU3RhcnQgPSBwO1xuICAgICAgICAgICAgdmFyIG5iaW4gPSByZWFkSW50KHVuY2JhLCBwKTsgcCArPSA0O1xuICAgICAgICAgICAgZm9yICh2YXIgYiA9IDA7IGIgPCBuYmluOyArK2IpIHtcbiAgICAgICAgICAgICAgICB2YXIgYmluID0gcmVhZEludCh1bmNiYSwgcCk7XG4gICAgICAgICAgICAgICAgdmFyIG5jaG5rID0gcmVhZEludCh1bmNiYSwgcCs0KTtcbiAgICAgICAgICAgICAgICBwICs9IDggKyAobmNobmsgKiAxNik7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICB2YXIgbmludHYgPSByZWFkSW50KHVuY2JhLCBwKTsgcCArPSA0O1xuICAgICAgICAgICAgXG4gICAgICAgICAgICB2YXIgcSA9IHA7XG4gICAgICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IG5pbnR2OyArK2kpIHtcbiAgICAgICAgICAgICAgICB2YXIgdiA9IHJlYWRWb2IodW5jYmEsIHEpOyBxICs9IDg7XG4gICAgICAgICAgICAgICAgaWYgKHYpIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGJpID0gdi5ibG9jaztcbiAgICAgICAgICAgICAgICAgICAgaWYgKHYub2Zmc2V0ID4gMClcbiAgICAgICAgICAgICAgICAgICAgICAgIGJpICs9IDY1NTM2O1xuXG4gICAgICAgICAgICAgICAgICAgIGlmIChiaSA8IG1pbkJsb2NrSW5kZXgpXG4gICAgICAgICAgICAgICAgICAgICAgICBtaW5CbG9ja0luZGV4ID0gYmk7XG4gICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHAgKz0gKG5pbnR2ICogOCk7XG5cblxuICAgICAgICAgICAgaWYgKG5iaW4gPiAwKSB7XG4gICAgICAgICAgICAgICAgYmFtLmluZGljZXNbcmVmXSA9IG5ldyBVaW50OEFycmF5KGhlYWRlciwgYmxvY2tTdGFydCwgcCAtIGJsb2NrU3RhcnQpO1xuICAgICAgICAgICAgfSAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICB9XG5cbiAgICAgICAgYmFtLmRhdGEuc2xpY2UoMCwgbWluQmxvY2tJbmRleCkuZmV0Y2goZnVuY3Rpb24ocikge1xuICAgICAgICAgICAgaWYgKCFyKSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKG51bGwsIFwiQ291bGRuJ3QgYWNjZXNzIEJBTVwiKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgdmFyIHVuYyA9IHVuYmd6ZihyLCByLmJ5dGVMZW5ndGgpO1xuICAgICAgICAgICAgdmFyIHVuY2JhID0gbmV3IFVpbnQ4QXJyYXkodW5jKTtcblxuICAgICAgICAgICAgdmFyIG1hZ2ljID0gcmVhZEludCh1bmNiYSwgMCk7XG4gICAgICAgICAgICBpZiAobWFnaWMgIT0gQkFNX01BR0lDKSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKG51bGwsIFwiTm90IGEgQkFNIGZpbGUsIG1hZ2ljPTB4XCIgKyBtYWdpYy50b1N0cmluZygxNikpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdmFyIGhlYWRMZW4gPSByZWFkSW50KHVuY2JhLCA0KTtcbiAgICAgICAgICAgIHZhciBoZWFkZXIgPSAnJztcbiAgICAgICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgaGVhZExlbjsgKytpKSB7XG4gICAgICAgICAgICAgICAgaGVhZGVyICs9IFN0cmluZy5mcm9tQ2hhckNvZGUodW5jYmFbaSArIDhdKTtcbiAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgdmFyIG5SZWYgPSByZWFkSW50KHVuY2JhLCBoZWFkTGVuICsgOCk7XG4gICAgICAgICAgICB2YXIgcCA9IGhlYWRMZW4gKyAxMjtcblxuICAgICAgICAgICAgYmFtLmNoclRvSW5kZXggPSB7fTtcbiAgICAgICAgICAgIGJhbS5pbmRleFRvQ2hyID0gW107XG4gICAgICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IG5SZWY7ICsraSkge1xuICAgICAgICAgICAgICAgIHZhciBsTmFtZSA9IHJlYWRJbnQodW5jYmEsIHApO1xuICAgICAgICAgICAgICAgIHZhciBuYW1lID0gJyc7XG4gICAgICAgICAgICAgICAgZm9yICh2YXIgaiA9IDA7IGogPCBsTmFtZS0xOyArK2opIHtcbiAgICAgICAgICAgICAgICAgICAgbmFtZSArPSBTdHJpbmcuZnJvbUNoYXJDb2RlKHVuY2JhW3AgKyA0ICsgal0pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB2YXIgbFJlZiA9IHJlYWRJbnQodW5jYmEsIHAgKyBsTmFtZSArIDQpO1xuICAgICAgICAgICAgICAgIGJhbS5jaHJUb0luZGV4W25hbWVdID0gaTtcbiAgICAgICAgICAgICAgICBpZiAobmFtZS5pbmRleE9mKCdjaHInKSA9PSAwKSB7XG4gICAgICAgICAgICAgICAgICAgIGJhbS5jaHJUb0luZGV4W25hbWUuc3Vic3RyaW5nKDMpXSA9IGk7XG4gICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgYmFtLmNoclRvSW5kZXhbJ2NocicgKyBuYW1lXSA9IGk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGJhbS5pbmRleFRvQ2hyLnB1c2gobmFtZSk7XG5cbiAgICAgICAgICAgICAgICBwID0gcCArIDggKyBsTmFtZTtcbiAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgaWYgKGJhbS5pbmRpY2VzKSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKGJhbSk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0pO1xuICAgIH0pO1xufVxuXG5cblxuQmFtRmlsZS5wcm90b3R5cGUuYmxvY2tzRm9yUmFuZ2UgPSBmdW5jdGlvbihyZWZJZCwgbWluLCBtYXgpIHtcbiAgICB2YXIgaW5kZXggPSB0aGlzLmluZGljZXNbcmVmSWRdO1xuICAgIGlmICghaW5kZXgpIHtcbiAgICAgICAgcmV0dXJuIFtdO1xuICAgIH1cblxuICAgIHZhciBpbnRCaW5zTCA9IHJlZzJiaW5zKG1pbiwgbWF4KTtcbiAgICB2YXIgaW50QmlucyA9IFtdO1xuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgaW50Qmluc0wubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgaW50Qmluc1tpbnRCaW5zTFtpXV0gPSB0cnVlO1xuICAgIH1cbiAgICB2YXIgbGVhZkNodW5rcyA9IFtdLCBvdGhlckNodW5rcyA9IFtdO1xuXG4gICAgdmFyIG5iaW4gPSByZWFkSW50KGluZGV4LCAwKTtcbiAgICB2YXIgcCA9IDQ7XG4gICAgZm9yICh2YXIgYiA9IDA7IGIgPCBuYmluOyArK2IpIHtcbiAgICAgICAgdmFyIGJpbiA9IHJlYWRJbnQoaW5kZXgsIHApO1xuICAgICAgICB2YXIgbmNobmsgPSByZWFkSW50KGluZGV4LCBwKzQpO1xuLy8gICAgICAgIGRsb2coJ2Jpbj0nICsgYmluICsgJzsgbmNobms9JyArIG5jaG5rKTtcbiAgICAgICAgcCArPSA4O1xuICAgICAgICBpZiAoaW50Qmluc1tiaW5dKSB7XG4gICAgICAgICAgICBmb3IgKHZhciBjID0gMDsgYyA8IG5jaG5rOyArK2MpIHtcbiAgICAgICAgICAgICAgICB2YXIgY3MgPSByZWFkVm9iKGluZGV4LCBwKTtcbiAgICAgICAgICAgICAgICB2YXIgY2UgPSByZWFkVm9iKGluZGV4LCBwICsgOCk7XG4gICAgICAgICAgICAgICAgKGJpbiA8IDQ2ODEgPyBvdGhlckNodW5rcyA6IGxlYWZDaHVua3MpLnB1c2gobmV3IENodW5rKGNzLCBjZSkpO1xuICAgICAgICAgICAgICAgIHAgKz0gMTY7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBwICs9ICAobmNobmsgKiAxNik7XG4gICAgICAgIH1cbiAgICB9XG4vLyAgICBkbG9nKCdsZWFmQ2h1bmtzID0gJyArIG1pbmlKU09OaWZ5KGxlYWZDaHVua3MpKTtcbi8vICAgIGRsb2coJ290aGVyQ2h1bmtzID0gJyArIG1pbmlKU09OaWZ5KG90aGVyQ2h1bmtzKSk7XG5cbiAgICB2YXIgbmludHYgPSByZWFkSW50KGluZGV4LCBwKTtcbiAgICB2YXIgbG93ZXN0ID0gbnVsbDtcbiAgICB2YXIgbWluTGluID0gTWF0aC5taW4obWluPj4xNCwgbmludHYgLSAxKSwgbWF4TGluID0gTWF0aC5taW4obWF4Pj4xNCwgbmludHYgLSAxKTtcbiAgICBmb3IgKHZhciBpID0gbWluTGluOyBpIDw9IG1heExpbjsgKytpKSB7XG4gICAgICAgIHZhciBsYiA9ICByZWFkVm9iKGluZGV4LCBwICsgNCArIChpICogOCkpO1xuICAgICAgICBpZiAoIWxiKSB7XG4gICAgICAgICAgICBjb250aW51ZTtcbiAgICAgICAgfVxuICAgICAgICBpZiAoIWxvd2VzdCB8fCBsYi5ibG9jayA8IGxvd2VzdC5ibG9jayB8fCBsYi5vZmZzZXQgPCBsb3dlc3Qub2Zmc2V0KSB7XG4gICAgICAgICAgICBsb3dlc3QgPSBsYjtcbiAgICAgICAgfVxuICAgIH1cbiAgICAvLyBkbG9nKCdMb3dlc3QgTEIgPSAnICsgbG93ZXN0KTtcbiAgICBcbiAgICB2YXIgcHJ1bmVkT3RoZXJDaHVua3MgPSBbXTtcbiAgICBpZiAobG93ZXN0ICE9IG51bGwpIHtcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBvdGhlckNodW5rcy5sZW5ndGg7ICsraSkge1xuICAgICAgICAgICAgdmFyIGNobmsgPSBvdGhlckNodW5rc1tpXTtcbiAgICAgICAgICAgIGlmIChjaG5rLm1heHYuYmxvY2sgPj0gbG93ZXN0LmJsb2NrICYmIGNobmsubWF4di5vZmZzZXQgPj0gbG93ZXN0Lm9mZnNldCkge1xuICAgICAgICAgICAgICAgIHBydW5lZE90aGVyQ2h1bmtzLnB1c2goY2huayk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9XG4gICAgLy8gZGxvZygncHJ1bmVkT3RoZXJDaHVua3MgPSAnICsgbWluaUpTT05pZnkocHJ1bmVkT3RoZXJDaHVua3MpKTtcbiAgICBvdGhlckNodW5rcyA9IHBydW5lZE90aGVyQ2h1bmtzO1xuXG4gICAgdmFyIGludENodW5rcyA9IFtdO1xuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgb3RoZXJDaHVua3MubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgaW50Q2h1bmtzLnB1c2gob3RoZXJDaHVua3NbaV0pO1xuICAgIH1cbiAgICBmb3IgKHZhciBpID0gMDsgaSA8IGxlYWZDaHVua3MubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgaW50Q2h1bmtzLnB1c2gobGVhZkNodW5rc1tpXSk7XG4gICAgfVxuXG4gICAgaW50Q2h1bmtzLnNvcnQoZnVuY3Rpb24oYzAsIGMxKSB7XG4gICAgICAgIHZhciBkaWYgPSBjMC5taW52LmJsb2NrIC0gYzEubWludi5ibG9jaztcbiAgICAgICAgaWYgKGRpZiAhPSAwKSB7XG4gICAgICAgICAgICByZXR1cm4gZGlmO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgcmV0dXJuIGMwLm1pbnYub2Zmc2V0IC0gYzEubWludi5vZmZzZXQ7XG4gICAgICAgIH1cbiAgICB9KTtcbiAgICB2YXIgbWVyZ2VkQ2h1bmtzID0gW107XG4gICAgaWYgKGludENodW5rcy5sZW5ndGggPiAwKSB7XG4gICAgICAgIHZhciBjdXIgPSBpbnRDaHVua3NbMF07XG4gICAgICAgIGZvciAodmFyIGkgPSAxOyBpIDwgaW50Q2h1bmtzLmxlbmd0aDsgKytpKSB7XG4gICAgICAgICAgICB2YXIgbmMgPSBpbnRDaHVua3NbaV07XG4gICAgICAgICAgICBpZiAobmMubWludi5ibG9jayA9PSBjdXIubWF4di5ibG9jayAvKiAmJiBuYy5taW52Lm9mZnNldCA9PSBjdXIubWF4di5vZmZzZXQgKi8pIHsgLy8gbm8gcG9pbnQgc3BsaXR0aW5nIG1pZC1ibG9ja1xuICAgICAgICAgICAgICAgIGN1ciA9IG5ldyBDaHVuayhjdXIubWludiwgbmMubWF4dik7XG4gICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgIG1lcmdlZENodW5rcy5wdXNoKGN1cik7XG4gICAgICAgICAgICAgICAgY3VyID0gbmM7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgbWVyZ2VkQ2h1bmtzLnB1c2goY3VyKTtcbiAgICB9XG4gICAgLy8gZGxvZygnbWVyZ2VkQ2h1bmtzID0gJyArIG1pbmlKU09OaWZ5KG1lcmdlZENodW5rcykpO1xuXG4gICAgcmV0dXJuIG1lcmdlZENodW5rcztcbn1cblxuQmFtRmlsZS5wcm90b3R5cGUuZmV0Y2ggPSBmdW5jdGlvbihjaHIsIG1pbiwgbWF4LCBjYWxsYmFjaywgb3B0cykge1xuICAgIHZhciB0aGlzQiA9IHRoaXM7XG4gICAgb3B0cyA9IG9wdHMgfHwge307XG5cbiAgICB2YXIgY2hySWQgPSB0aGlzLmNoclRvSW5kZXhbY2hyXTtcbiAgICB2YXIgY2h1bmtzO1xuICAgIGlmIChjaHJJZCA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgIGNodW5rcyA9IFtdO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIGNodW5rcyA9IHRoaXMuYmxvY2tzRm9yUmFuZ2UoY2hySWQsIG1pbiwgbWF4KTtcbiAgICAgICAgaWYgKCFjaHVua3MpIHtcbiAgICAgICAgICAgIGNhbGxiYWNrKG51bGwsICdFcnJvciBpbiBpbmRleCBmZXRjaCcpO1xuICAgICAgICB9XG4gICAgfVxuICAgIFxuICAgIHZhciByZWNvcmRzID0gW107XG4gICAgdmFyIGluZGV4ID0gMDtcbiAgICB2YXIgZGF0YTtcblxuICAgIGZ1bmN0aW9uIHRyYW1wKCkge1xuICAgICAgICBpZiAoaW5kZXggPj0gY2h1bmtzLmxlbmd0aCkge1xuICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKHJlY29yZHMpO1xuICAgICAgICB9IGVsc2UgaWYgKCFkYXRhKSB7XG4gICAgICAgICAgICAvLyBkbG9nKCdmZXRjaGluZyAnICsgaW5kZXgpO1xuICAgICAgICAgICAgdmFyIGMgPSBjaHVua3NbaW5kZXhdO1xuICAgICAgICAgICAgdmFyIGZldGNoTWluID0gYy5taW52LmJsb2NrO1xuICAgICAgICAgICAgdmFyIGZldGNoTWF4ID0gYy5tYXh2LmJsb2NrICsgKDE8PDE2KTsgLy8gKnNpZ2gqXG4gICAgICAgICAgICB0aGlzQi5kYXRhLnNsaWNlKGZldGNoTWluLCBmZXRjaE1heCAtIGZldGNoTWluKS5mZXRjaChmdW5jdGlvbihyKSB7XG4gICAgICAgICAgICAgICAgZGF0YSA9IHVuYmd6ZihyLCBjLm1heHYuYmxvY2sgLSBjLm1pbnYuYmxvY2sgKyAxKTtcbiAgICAgICAgICAgICAgICByZXR1cm4gdHJhbXAoKTtcbiAgICAgICAgICAgIH0pO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgdmFyIGJhID0gbmV3IFVpbnQ4QXJyYXkoZGF0YSk7XG4gICAgICAgICAgICB0aGlzQi5yZWFkQmFtUmVjb3JkcyhiYSwgY2h1bmtzW2luZGV4XS5taW52Lm9mZnNldCwgcmVjb3JkcywgbWluLCBtYXgsIGNocklkLCBvcHRzKTtcbiAgICAgICAgICAgIGRhdGEgPSBudWxsO1xuICAgICAgICAgICAgKytpbmRleDtcbiAgICAgICAgICAgIHJldHVybiB0cmFtcCgpO1xuICAgICAgICB9XG4gICAgfVxuICAgIHRyYW1wKCk7XG59XG5cbnZhciBTRVFSRVRfREVDT0RFUiA9IFsnPScsICdBJywgJ0MnLCAneCcsICdHJywgJ3gnLCAneCcsICd4JywgJ1QnLCAneCcsICd4JywgJ3gnLCAneCcsICd4JywgJ3gnLCAnTiddO1xudmFyIENJR0FSX0RFQ09ERVIgPSBbJ00nLCAnSScsICdEJywgJ04nLCAnUycsICdIJywgJ1AnLCAnPScsICdYJywgJz8nLCAnPycsICc/JywgJz8nLCAnPycsICc/JywgJz8nXTtcblxuZnVuY3Rpb24gQmFtUmVjb3JkKCkge1xufVxuXG5CYW1GaWxlLnByb3RvdHlwZS5yZWFkQmFtUmVjb3JkcyA9IGZ1bmN0aW9uKGJhLCBvZmZzZXQsIHNpbmssIG1pbiwgbWF4LCBjaHJJZCwgb3B0cykge1xuICAgIHdoaWxlICh0cnVlKSB7XG4gICAgICAgIHZhciBibG9ja1NpemUgPSByZWFkSW50KGJhLCBvZmZzZXQpO1xuICAgICAgICB2YXIgYmxvY2tFbmQgPSBvZmZzZXQgKyBibG9ja1NpemUgKyA0O1xuICAgICAgICBpZiAoYmxvY2tFbmQgPj0gYmEubGVuZ3RoKSB7XG4gICAgICAgICAgICByZXR1cm4gc2luaztcbiAgICAgICAgfVxuXG4gICAgICAgIHZhciByZWNvcmQgPSBuZXcgQmFtUmVjb3JkKCk7XG5cbiAgICAgICAgdmFyIHJlZklEID0gcmVhZEludChiYSwgb2Zmc2V0ICsgNCk7XG4gICAgICAgIHZhciBwb3MgPSByZWFkSW50KGJhLCBvZmZzZXQgKyA4KTtcbiAgICAgICAgXG4gICAgICAgIHZhciBibW4gPSByZWFkSW50KGJhLCBvZmZzZXQgKyAxMik7XG4gICAgICAgIHZhciBiaW4gPSAoYm1uICYgMHhmZmZmMDAwMCkgPj4gMTY7XG4gICAgICAgIHZhciBtcSA9IChibW4gJiAweGZmMDApID4+IDg7XG4gICAgICAgIHZhciBubCA9IGJtbiAmIDB4ZmY7XG5cbiAgICAgICAgdmFyIGZsYWdfbmMgPSByZWFkSW50KGJhLCBvZmZzZXQgKyAxNik7XG4gICAgICAgIHZhciBmbGFnID0gKGZsYWdfbmMgJiAweGZmZmYwMDAwKSA+PiAxNjtcbiAgICAgICAgdmFyIG5jID0gZmxhZ19uYyAmIDB4ZmZmZjtcbiAgICBcbiAgICAgICAgdmFyIGxzZXEgPSByZWFkSW50KGJhLCBvZmZzZXQgKyAyMCk7XG4gICAgICAgIFxuICAgICAgICB2YXIgbmV4dFJlZiAgPSByZWFkSW50KGJhLCBvZmZzZXQgKyAyNCk7XG4gICAgICAgIHZhciBuZXh0UG9zID0gcmVhZEludChiYSwgb2Zmc2V0ICsgMjgpO1xuICAgICAgICBcbiAgICAgICAgdmFyIHRsZW4gPSByZWFkSW50KGJhLCBvZmZzZXQgKyAzMik7XG4gICAgXG4gICAgICAgIHJlY29yZC5zZWdtZW50ID0gdGhpcy5pbmRleFRvQ2hyW3JlZklEXTtcbiAgICAgICAgcmVjb3JkLmZsYWcgPSBmbGFnO1xuICAgICAgICByZWNvcmQucG9zID0gcG9zO1xuICAgICAgICByZWNvcmQubXEgPSBtcTtcbiAgICAgICAgaWYgKG9wdHMubGlnaHQpXG4gICAgICAgICAgICByZWNvcmQuc2VxTGVuZ3RoID0gbHNlcTtcblxuICAgICAgICBpZiAoIW9wdHMubGlnaHQpIHtcbiAgICAgICAgICAgIGlmIChuZXh0UmVmID49IDApIHtcbiAgICAgICAgICAgICAgICByZWNvcmQubmV4dFNlZ21lbnQgPSB0aGlzLmluZGV4VG9DaHJbbmV4dFJlZl07XG4gICAgICAgICAgICAgICAgcmVjb3JkLm5leHRQb3MgPSBuZXh0UG9zO1xuICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICB2YXIgcmVhZE5hbWUgPSAnJztcbiAgICAgICAgICAgIGZvciAodmFyIGogPSAwOyBqIDwgbmwtMTsgKytqKSB7XG4gICAgICAgICAgICAgICAgcmVhZE5hbWUgKz0gU3RyaW5nLmZyb21DaGFyQ29kZShiYVtvZmZzZXQgKyAzNiArIGpdKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJlY29yZC5yZWFkTmFtZSA9IHJlYWROYW1lO1xuICAgICAgICBcbiAgICAgICAgICAgIHZhciBwID0gb2Zmc2V0ICsgMzYgKyBubDtcblxuICAgICAgICAgICAgdmFyIGNpZ2FyID0gJyc7XG4gICAgICAgICAgICBmb3IgKHZhciBjID0gMDsgYyA8IG5jOyArK2MpIHtcbiAgICAgICAgICAgICAgICB2YXIgY2lnb3AgPSByZWFkSW50KGJhLCBwKTtcbiAgICAgICAgICAgICAgICBjaWdhciA9IGNpZ2FyICsgKGNpZ29wPj40KSArIENJR0FSX0RFQ09ERVJbY2lnb3AgJiAweGZdO1xuICAgICAgICAgICAgICAgIHAgKz0gNDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJlY29yZC5jaWdhciA9IGNpZ2FyO1xuICAgICAgICBcbiAgICAgICAgICAgIHZhciBzZXEgPSAnJztcbiAgICAgICAgICAgIHZhciBzZXFCeXRlcyA9IChsc2VxICsgMSkgPj4gMTtcbiAgICAgICAgICAgIGZvciAodmFyIGogPSAwOyBqIDwgc2VxQnl0ZXM7ICsraikge1xuICAgICAgICAgICAgICAgIHZhciBzYiA9IGJhW3AgKyBqXTtcbiAgICAgICAgICAgICAgICBzZXEgKz0gU0VRUkVUX0RFQ09ERVJbKHNiICYgMHhmMCkgPj4gNF07XG4gICAgICAgICAgICAgICAgc2VxICs9IFNFUVJFVF9ERUNPREVSWyhzYiAmIDB4MGYpXTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHAgKz0gc2VxQnl0ZXM7XG4gICAgICAgICAgICByZWNvcmQuc2VxID0gc2VxO1xuXG4gICAgICAgICAgICB2YXIgcXNlcSA9ICcnO1xuICAgICAgICAgICAgZm9yICh2YXIgaiA9IDA7IGogPCBsc2VxOyArK2opIHtcbiAgICAgICAgICAgICAgICBxc2VxICs9IFN0cmluZy5mcm9tQ2hhckNvZGUoYmFbcCArIGpdICsgMzMpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcCArPSBsc2VxO1xuICAgICAgICAgICAgcmVjb3JkLnF1YWxzID0gcXNlcTtcblxuICAgICAgICAgICAgd2hpbGUgKHAgPCBibG9ja0VuZCkge1xuICAgICAgICAgICAgICAgIHZhciB0YWcgPSBTdHJpbmcuZnJvbUNoYXJDb2RlKGJhW3BdLCBiYVtwICsgMV0pO1xuICAgICAgICAgICAgICAgIHZhciB0eXBlID0gU3RyaW5nLmZyb21DaGFyQ29kZShiYVtwICsgMl0pO1xuICAgICAgICAgICAgICAgIHZhciB2YWx1ZTtcblxuICAgICAgICAgICAgICAgIGlmICh0eXBlID09ICdBJykge1xuICAgICAgICAgICAgICAgICAgICB2YWx1ZSA9IFN0cmluZy5mcm9tQ2hhckNvZGUoYmFbcCArIDNdKTtcbiAgICAgICAgICAgICAgICAgICAgcCArPSA0O1xuICAgICAgICAgICAgICAgIH0gZWxzZSBpZiAodHlwZSA9PSAnaScgfHwgdHlwZSA9PSAnSScpIHtcbiAgICAgICAgICAgICAgICAgICAgdmFsdWUgPSByZWFkSW50KGJhLCBwICsgMyk7XG4gICAgICAgICAgICAgICAgICAgIHAgKz0gNztcbiAgICAgICAgICAgICAgICB9IGVsc2UgaWYgKHR5cGUgPT0gJ2MnIHx8IHR5cGUgPT0gJ0MnKSB7XG4gICAgICAgICAgICAgICAgICAgIHZhbHVlID0gYmFbcCArIDNdO1xuICAgICAgICAgICAgICAgICAgICBwICs9IDQ7XG4gICAgICAgICAgICAgICAgfSBlbHNlIGlmICh0eXBlID09ICdzJyB8fCB0eXBlID09ICdTJykge1xuICAgICAgICAgICAgICAgICAgICB2YWx1ZSA9IHJlYWRTaG9ydChiYSwgcCArIDMpO1xuICAgICAgICAgICAgICAgICAgICBwICs9IDU7XG4gICAgICAgICAgICAgICAgfSBlbHNlIGlmICh0eXBlID09ICdmJykge1xuICAgICAgICAgICAgICAgICAgICB2YWx1ZSA9IHJlYWRGbG9hdChiYSwgcCArIDMpO1xuICAgICAgICAgICAgICAgICAgICBwICs9IDc7XG4gICAgICAgICAgICAgICAgfSBlbHNlIGlmICh0eXBlID09ICdaJyB8fCB0eXBlID09ICdIJykge1xuICAgICAgICAgICAgICAgICAgICBwICs9IDM7XG4gICAgICAgICAgICAgICAgICAgIHZhbHVlID0gJyc7XG4gICAgICAgICAgICAgICAgICAgIGZvciAoOzspIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciBjYyA9IGJhW3ArK107XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoY2MgPT0gMCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZSArPSBTdHJpbmcuZnJvbUNoYXJDb2RlKGNjKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH0gZWxzZSBpZiAodHlwZSA9PSAnQicpIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGF0eXBlID0gU3RyaW5nLmZyb21DaGFyQ29kZShiYVtwICsgM10pO1xuICAgICAgICAgICAgICAgICAgICB2YXIgYWxlbiA9IHJlYWRJbnQoYmEsIHAgKyA0KTtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGVsZW47XG4gICAgICAgICAgICAgICAgICAgIHZhciByZWFkZXI7XG4gICAgICAgICAgICAgICAgICAgIGlmIChhdHlwZSA9PSAnaScgfHwgYXR5cGUgPT0gJ0knIHx8IGF0eXBlID09ICdmJykge1xuICAgICAgICAgICAgICAgICAgICAgICAgZWxlbiA9IDQ7XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoYXR5cGUgPT0gJ2YnKVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJlYWRlciA9IHJlYWRGbG9hdDtcbiAgICAgICAgICAgICAgICAgICAgICAgIGVsc2VcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICByZWFkZXIgPSByZWFkSW50O1xuICAgICAgICAgICAgICAgICAgICB9IGVsc2UgaWYgKGF0eXBlID09ICdzJyB8fCBhdHlwZSA9PSAnUycpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGVsZW4gPSAyO1xuICAgICAgICAgICAgICAgICAgICAgICAgcmVhZGVyID0gcmVhZFNob3J0O1xuICAgICAgICAgICAgICAgICAgICB9IGVsc2UgaWYgKGF0eXBlID09ICdjJyB8fCBhdHlwZSA9PSAnQycpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGVsZW4gPSAxO1xuICAgICAgICAgICAgICAgICAgICAgICAgcmVhZGVyID0gcmVhZEJ5dGU7XG4gICAgICAgICAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB0aHJvdyAnVW5rbm93biBhcnJheSB0eXBlICcgKyBhdHlwZTtcbiAgICAgICAgICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICAgICAgICAgIHAgKz0gODtcbiAgICAgICAgICAgICAgICAgICAgdmFsdWUgPSBbXTtcbiAgICAgICAgICAgICAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBhbGVuOyArK2kpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHZhbHVlLnB1c2gocmVhZGVyKGJhLCBwKSk7XG4gICAgICAgICAgICAgICAgICAgICAgICBwICs9IGVsZW47XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICB0aHJvdyAnVW5rbm93biB0eXBlICcrIHR5cGU7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHJlY29yZFt0YWddID0gdmFsdWU7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cblxuICAgICAgICBpZiAoIW1pbiB8fCByZWNvcmQucG9zIDw9IG1heCAmJiByZWNvcmQucG9zICsgbHNlcSA+PSBtaW4pIHtcbiAgICAgICAgICAgIGlmIChjaHJJZCA9PT0gdW5kZWZpbmVkIHx8IHJlZklEID09IGNocklkKSB7XG4gICAgICAgICAgICAgICAgc2luay5wdXNoKHJlY29yZCk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgb2Zmc2V0ID0gYmxvY2tFbmQ7XG4gICAgfVxuXG4gICAgLy8gRXhpdHMgdmlhIHRvcCBvZiBsb29wLlxufTtcblxuaWYgKHR5cGVvZihtb2R1bGUpICE9PSAndW5kZWZpbmVkJykge1xuICAgIG1vZHVsZS5leHBvcnRzID0ge1xuICAgICAgICBtYWtlQmFtOiBtYWtlQmFtLFxuICAgICAgICBCQU1fTUFHSUM6IEJBTV9NQUdJQyxcbiAgICAgICAgQkFJX01BR0lDOiBCQUlfTUFHSUMsXG4gICAgICAgIEJhbUZsYWdzOiBCYW1GbGFnc1xuICAgIH07XG59IiwiLyogLSotIG1vZGU6IGphdmFzY3JpcHQ7IGMtYmFzaWMtb2Zmc2V0OiA0OyBpbmRlbnQtdGFicy1tb2RlOiBuaWwgLSotICovXG5cbi8vIFxuLy8gRGFsbGlhbmNlIEdlbm9tZSBFeHBsb3JlclxuLy8gKGMpIFRob21hcyBEb3duIDIwMDYtMjAxMFxuLy9cbi8vIGJpZ3dpZy5qczogaW5kZXhlZCBiaW5hcnkgV0lHIChhbmQgQkVEKSBmaWxlc1xuLy9cblxuXCJ1c2Ugc3RyaWN0XCI7XG5cblxuaWYgKHR5cGVvZihyZXF1aXJlKSAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICB2YXIgc3BhbnMgPSByZXF1aXJlKCcuL3NwYW5zJyk7XG4gICAgdmFyIFJhbmdlID0gc3BhbnMuUmFuZ2U7XG4gICAgdmFyIHVuaW9uID0gc3BhbnMudW5pb247XG4gICAgdmFyIGludGVyc2VjdGlvbiA9IHNwYW5zLmludGVyc2VjdGlvbjtcblxuICAgIHZhciBkYXMgPSByZXF1aXJlKCcuL2RhcycpO1xuICAgIHZhciBEQVNGZWF0dXJlID0gZGFzLkRBU0ZlYXR1cmU7XG4gICAgdmFyIERBU0dyb3VwID0gZGFzLkRBU0dyb3VwO1xuXG4gICAgdmFyIHV0aWxzID0gcmVxdWlyZSgnLi91dGlscycpO1xuICAgIHZhciBzaGFsbG93Q29weSA9IHV0aWxzLnNoYWxsb3dDb3B5O1xuXG4gICAgdmFyIGJpbiA9IHJlcXVpcmUoJy4vYmluJyk7XG4gICAgdmFyIHJlYWRJbnQgPSBiaW4ucmVhZEludDtcblxuICAgIHZhciBqc3psaWIgPSByZXF1aXJlKCdqc3psaWInKTtcbiAgICB2YXIganN6bGliX2luZmxhdGVfYnVmZmVyID0ganN6bGliLmluZmxhdGVCdWZmZXI7XG4gICAgdmFyIGFycmF5Q29weSA9IGpzemxpYi5hcnJheUNvcHk7XG59XG5cbnZhciBCSUdfV0lHX01BR0lDID0gMHg4ODhGRkMyNjtcbnZhciBCSUdfV0lHX01BR0lDX0JFID0gMHgyNkZDOEY4ODtcbnZhciBCSUdfQkVEX01BR0lDID0gMHg4Nzg5RjJFQjtcbnZhciBCSUdfQkVEX01BR0lDX0JFID0gMHhFQkYyODk4NztcblxuXG52YXIgQklHX1dJR19UWVBFX0dSQVBIID0gMTtcbnZhciBCSUdfV0lHX1RZUEVfVlNURVAgPSAyO1xudmFyIEJJR19XSUdfVFlQRV9GU1RFUCA9IDM7XG4gIFxudmFyIE0xID0gMjU2O1xudmFyIE0yID0gMjU2KjI1NjtcbnZhciBNMyA9IDI1NioyNTYqMjU2O1xudmFyIE00ID0gMjU2KjI1NioyNTYqMjU2O1xuXG52YXIgQkVEX0NPTE9SX1JFR0VYUCA9IG5ldyBSZWdFeHAoXCJeWzAtOV0rLFswLTldKyxbMC05XStcIik7XG5cbmZ1bmN0aW9uIGJ3Z19yZWFkT2Zmc2V0KGJhLCBvKSB7XG4gICAgdmFyIG9mZnNldCA9IGJhW29dICsgYmFbbysxXSpNMSArIGJhW28rMl0qTTIgKyBiYVtvKzNdKk0zICsgYmFbbys0XSpNNDtcbiAgICByZXR1cm4gb2Zmc2V0O1xufVxuXG5mdW5jdGlvbiBCaWdXaWcoKSB7XG59XG5cbkJpZ1dpZy5wcm90b3R5cGUucmVhZENocm9tVHJlZSA9IGZ1bmN0aW9uKGNhbGxiYWNrKSB7XG4gICAgdmFyIHRoaXNCID0gdGhpcztcbiAgICB0aGlzLmNocm9tc1RvSURzID0ge307XG4gICAgdGhpcy5pZHNUb0Nocm9tcyA9IHt9O1xuICAgIHRoaXMubWF4SUQgPSAwO1xuXG4gICAgdmFyIHVkbyA9IHRoaXMudW56b29tZWREYXRhT2Zmc2V0O1xuICAgIHZhciBlYiA9ICh1ZG8gLSB0aGlzLmNocm9tVHJlZU9mZnNldCkgJiAzO1xuICAgIHVkbyA9IHVkbyArIDQgLSBlYjtcblxuICAgIHRoaXMuZGF0YS5zbGljZSh0aGlzLmNocm9tVHJlZU9mZnNldCwgdWRvIC0gdGhpcy5jaHJvbVRyZWVPZmZzZXQpLmZldGNoKGZ1bmN0aW9uKGJwdCkge1xuICAgICAgICB2YXIgYmEgPSBuZXcgVWludDhBcnJheShicHQpO1xuICAgICAgICB2YXIgc2EgPSBuZXcgSW50MTZBcnJheShicHQpO1xuICAgICAgICB2YXIgbGEgPSBuZXcgSW50MzJBcnJheShicHQpO1xuICAgICAgICB2YXIgYnB0TWFnaWMgPSBsYVswXTtcbiAgICAgICAgdmFyIGJsb2NrU2l6ZSA9IGxhWzFdO1xuICAgICAgICB2YXIga2V5U2l6ZSA9IGxhWzJdO1xuICAgICAgICB2YXIgdmFsU2l6ZSA9IGxhWzNdO1xuICAgICAgICB2YXIgaXRlbUNvdW50ID0gYndnX3JlYWRPZmZzZXQoYmEsIDE2KTtcbiAgICAgICAgdmFyIHJvb3ROb2RlT2Zmc2V0ID0gMzI7XG5cbiAgICAgICAgdmFyIGJwdFJlYWROb2RlID0gZnVuY3Rpb24ob2Zmc2V0KSB7XG4gICAgICAgICAgICB2YXIgbm9kZVR5cGUgPSBiYVtvZmZzZXRdO1xuICAgICAgICAgICAgdmFyIGNudCA9IHNhWyhvZmZzZXQvMikgKyAxXTtcbiAgICAgICAgICAgIG9mZnNldCArPSA0O1xuICAgICAgICAgICAgZm9yICh2YXIgbiA9IDA7IG4gPCBjbnQ7ICsrbikge1xuICAgICAgICAgICAgICAgIGlmIChub2RlVHlwZSA9PSAwKSB7XG4gICAgICAgICAgICAgICAgICAgIG9mZnNldCArPSBrZXlTaXplO1xuICAgICAgICAgICAgICAgICAgICB2YXIgY2hpbGRPZmZzZXQgPSBid2dfcmVhZE9mZnNldChiYSwgb2Zmc2V0KTtcbiAgICAgICAgICAgICAgICAgICAgb2Zmc2V0ICs9IDg7XG4gICAgICAgICAgICAgICAgICAgIGNoaWxkT2Zmc2V0IC09IHRoaXNCLmNocm9tVHJlZU9mZnNldDtcbiAgICAgICAgICAgICAgICAgICAgYnB0UmVhZE5vZGUoY2hpbGRPZmZzZXQpO1xuICAgICAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciBrZXkgPSAnJztcbiAgICAgICAgICAgICAgICAgICAgZm9yICh2YXIga2kgPSAwOyBraSA8IGtleVNpemU7ICsra2kpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciBjaGFyQ29kZSA9IGJhW29mZnNldCsrXTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmIChjaGFyQ29kZSAhPSAwKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAga2V5ICs9IFN0cmluZy5mcm9tQ2hhckNvZGUoY2hhckNvZGUpO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIHZhciBjaHJvbUlkID0gKGJhW29mZnNldCszXTw8MjQpIHwgKGJhW29mZnNldCsyXTw8MTYpIHwgKGJhW29mZnNldCsxXTw8OCkgfCAoYmFbb2Zmc2V0KzBdKTtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGNocm9tU2l6ZSA9IChiYVtvZmZzZXQgKyA3XTw8MjQpIHwgKGJhW29mZnNldCs2XTw8MTYpIHwgKGJhW29mZnNldCs1XTw8OCkgfCAoYmFbb2Zmc2V0KzRdKTtcbiAgICAgICAgICAgICAgICAgICAgb2Zmc2V0ICs9IDg7XG5cbiAgICAgICAgICAgICAgICAgICAgdGhpc0IuY2hyb21zVG9JRHNba2V5XSA9IGNocm9tSWQ7XG4gICAgICAgICAgICAgICAgICAgIGlmIChrZXkuaW5kZXhPZignY2hyJykgPT0gMCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgdGhpc0IuY2hyb21zVG9JRHNba2V5LnN1YnN0cigzKV0gPSBjaHJvbUlkO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIHRoaXNCLmlkc1RvQ2hyb21zW2Nocm9tSWRdID0ga2V5O1xuICAgICAgICAgICAgICAgICAgICB0aGlzQi5tYXhJRCA9IE1hdGgubWF4KHRoaXNCLm1heElELCBjaHJvbUlkKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH07XG4gICAgICAgIGJwdFJlYWROb2RlKHJvb3ROb2RlT2Zmc2V0KTtcblxuICAgICAgICBjYWxsYmFjayh0aGlzQik7XG4gICAgfSk7XG59XG5cbmZ1bmN0aW9uIEJpZ1dpZ1ZpZXcoYndnLCBjaXJUcmVlT2Zmc2V0LCBjaXJUcmVlTGVuZ3RoLCBpc1N1bW1hcnkpIHtcbiAgICB0aGlzLmJ3ZyA9IGJ3ZztcbiAgICB0aGlzLmNpclRyZWVPZmZzZXQgPSBjaXJUcmVlT2Zmc2V0O1xuICAgIHRoaXMuY2lyVHJlZUxlbmd0aCA9IGNpclRyZWVMZW5ndGg7XG4gICAgdGhpcy5pc1N1bW1hcnkgPSBpc1N1bW1hcnk7XG59XG5cblxuXG5CaWdXaWdWaWV3LnByb3RvdHlwZS5yZWFkV2lnRGF0YSA9IGZ1bmN0aW9uKGNock5hbWUsIG1pbiwgbWF4LCBjYWxsYmFjaykge1xuICAgIHZhciBjaHIgPSB0aGlzLmJ3Zy5jaHJvbXNUb0lEc1tjaHJOYW1lXTtcbiAgICBpZiAoY2hyID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgLy8gTm90IGFuIGVycm9yIGJlY2F1c2Ugc29tZSAuYndncyB3b24ndCBoYXZlIGRhdGEgZm9yIGFsbCBjaHJvbW9zb21lcy5cblxuICAgICAgICAvLyBkbG9nKFwiQ291bGRuJ3QgZmluZCBjaHIgXCIgKyBjaHJOYW1lKTtcbiAgICAgICAgLy8gZGxvZygnQ2hyb21zPScgKyBtaW5pSlNPTmlmeSh0aGlzLmJ3Zy5jaHJvbXNUb0lEcykpO1xuICAgICAgICByZXR1cm4gY2FsbGJhY2soW10pO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHRoaXMucmVhZFdpZ0RhdGFCeUlkKGNociwgbWluLCBtYXgsIGNhbGxiYWNrKTtcbiAgICB9XG59XG5cbkJpZ1dpZ1ZpZXcucHJvdG90eXBlLnJlYWRXaWdEYXRhQnlJZCA9IGZ1bmN0aW9uKGNociwgbWluLCBtYXgsIGNhbGxiYWNrKSB7XG4gICAgdmFyIHRoaXNCID0gdGhpcztcbiAgICBpZiAoIXRoaXMuY2lySGVhZGVyKSB7XG4gICAgICAgIHRoaXMuYndnLmRhdGEuc2xpY2UodGhpcy5jaXJUcmVlT2Zmc2V0LCA0OCkuZmV0Y2goZnVuY3Rpb24ocmVzdWx0KSB7XG4gICAgICAgICAgICB0aGlzQi5jaXJIZWFkZXIgPSByZXN1bHQ7XG4gICAgICAgICAgICB2YXIgbGEgPSBuZXcgSW50MzJBcnJheSh0aGlzQi5jaXJIZWFkZXIpO1xuICAgICAgICAgICAgdGhpc0IuY2lyQmxvY2tTaXplID0gbGFbMV07XG4gICAgICAgICAgICB0aGlzQi5yZWFkV2lnRGF0YUJ5SWQoY2hyLCBtaW4sIG1heCwgY2FsbGJhY2spO1xuICAgICAgICB9KTtcbiAgICAgICAgcmV0dXJuO1xuICAgIH1cblxuICAgIHZhciBibG9ja3NUb0ZldGNoID0gW107XG4gICAgdmFyIG91dHN0YW5kaW5nID0gMDtcblxuICAgIHZhciBiZWZvcmVCV0cgPSBEYXRlLm5vdygpO1xuXG4gICAgdmFyIGZpbHRlciA9IGZ1bmN0aW9uKGNocm9tSWQsIGZtaW4sIGZtYXgsIHRva3MpIHtcbiAgICAgICAgcmV0dXJuICgoY2hyIDwgMCB8fCBjaHJvbUlkID09IGNocikgJiYgZm1pbiA8PSBtYXggJiYgZm1heCA+PSBtaW4pO1xuICAgIH1cblxuICAgIHZhciBjaXJGb2JSZWN1ciA9IGZ1bmN0aW9uKG9mZnNldCwgbGV2ZWwpIHtcbiAgICAgICAgaWYgKHRoaXNCLmJ3Zy5pbnN0cnVtZW50KVxuICAgICAgICAgICAgY29uc29sZS5sb2coJ2xldmVsPScgKyBsZXZlbCArICc7IG9mZnNldD0nICsgb2Zmc2V0ICsgJzsgdGltZT0nICsgKERhdGUubm93KCl8MCkpO1xuXG4gICAgICAgIG91dHN0YW5kaW5nICs9IG9mZnNldC5sZW5ndGg7XG5cbiAgICAgICAgaWYgKG9mZnNldC5sZW5ndGggPT0gMSAmJiBvZmZzZXRbMF0gLSB0aGlzQi5jaXJUcmVlT2Zmc2V0ID09IDQ4ICYmIHRoaXNCLmNhY2hlZENpclJvb3QpIHtcbiAgICAgICAgICAgIGNpckZvYlJlY3VyMih0aGlzQi5jYWNoZWRDaXJSb290LCAwLCBsZXZlbCk7XG4gICAgICAgICAgICAtLW91dHN0YW5kaW5nO1xuICAgICAgICAgICAgaWYgKG91dHN0YW5kaW5nID09IDApIHtcbiAgICAgICAgICAgICAgICB0aGlzQi5mZXRjaEZlYXR1cmVzKGZpbHRlciwgYmxvY2tzVG9GZXRjaCwgY2FsbGJhY2spO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG5cbiAgICAgICAgdmFyIG1heENpckJsb2NrU3BhbiA9IDQgKyAgKHRoaXNCLmNpckJsb2NrU2l6ZSAqIDMyKTsgICAvLyBVcHBlciBib3VuZCBvbiBzaXplLCBiYXNlZCBvbiBhIGNvbXBsZXRlbHkgZnVsbCBsZWFmIG5vZGUuXG4gICAgICAgIHZhciBzcGFucztcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBvZmZzZXQubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgICAgIHZhciBibG9ja1NwYW4gPSBuZXcgUmFuZ2Uob2Zmc2V0W2ldLCBvZmZzZXRbaV0gKyBtYXhDaXJCbG9ja1NwYW4pO1xuICAgICAgICAgICAgc3BhbnMgPSBzcGFucyA/IHVuaW9uKHNwYW5zLCBibG9ja1NwYW4pIDogYmxvY2tTcGFuO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgZmV0Y2hSYW5nZXMgPSBzcGFucy5yYW5nZXMoKTtcbiAgICAgICAgZm9yICh2YXIgciA9IDA7IHIgPCBmZXRjaFJhbmdlcy5sZW5ndGg7ICsrcikge1xuICAgICAgICAgICAgdmFyIGZyID0gZmV0Y2hSYW5nZXNbcl07XG4gICAgICAgICAgICBjaXJGb2JTdGFydEZldGNoKG9mZnNldCwgZnIsIGxldmVsKTtcbiAgICAgICAgfVxuICAgIH1cblxuICAgIHZhciBjaXJGb2JTdGFydEZldGNoID0gZnVuY3Rpb24ob2Zmc2V0LCBmciwgbGV2ZWwsIGF0dGVtcHRzKSB7XG4gICAgICAgIHZhciBsZW5ndGggPSBmci5tYXgoKSAtIGZyLm1pbigpO1xuICAgICAgICB0aGlzQi5id2cuZGF0YS5zbGljZShmci5taW4oKSwgZnIubWF4KCkgLSBmci5taW4oKSkuZmV0Y2goZnVuY3Rpb24ocmVzdWx0QnVmZmVyKSB7XG4gICAgICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IG9mZnNldC5sZW5ndGg7ICsraSkge1xuICAgICAgICAgICAgICAgIGlmIChmci5jb250YWlucyhvZmZzZXRbaV0pKSB7XG4gICAgICAgICAgICAgICAgICAgIGNpckZvYlJlY3VyMihyZXN1bHRCdWZmZXIsIG9mZnNldFtpXSAtIGZyLm1pbigpLCBsZXZlbCk7XG5cbiAgICAgICAgICAgICAgICAgICAgaWYgKG9mZnNldFtpXSAtIHRoaXNCLmNpclRyZWVPZmZzZXQgPT0gNDggJiYgb2Zmc2V0W2ldIC0gZnIubWluKCkgPT0gMClcbiAgICAgICAgICAgICAgICAgICAgICAgIHRoaXNCLmNhY2hlZENpclJvb3QgPSByZXN1bHRCdWZmZXI7XG5cbiAgICAgICAgICAgICAgICAgICAgLS1vdXRzdGFuZGluZztcbiAgICAgICAgICAgICAgICAgICAgaWYgKG91dHN0YW5kaW5nID09IDApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHRoaXNCLmZldGNoRmVhdHVyZXMoZmlsdGVyLCBibG9ja3NUb0ZldGNoLCBjYWxsYmFjayk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHZhciBjaXJGb2JSZWN1cjIgPSBmdW5jdGlvbihjaXJCbG9ja0RhdGEsIG9mZnNldCwgbGV2ZWwpIHtcbiAgICAgICAgdmFyIGJhID0gbmV3IFVpbnQ4QXJyYXkoY2lyQmxvY2tEYXRhKTtcbiAgICAgICAgdmFyIHNhID0gbmV3IEludDE2QXJyYXkoY2lyQmxvY2tEYXRhKTtcbiAgICAgICAgdmFyIGxhID0gbmV3IEludDMyQXJyYXkoY2lyQmxvY2tEYXRhKTtcblxuICAgICAgICB2YXIgaXNMZWFmID0gYmFbb2Zmc2V0XTtcbiAgICAgICAgdmFyIGNudCA9IHNhW29mZnNldC8yICsgMV07XG4gICAgICAgIG9mZnNldCArPSA0O1xuXG4gICAgICAgIGlmIChpc0xlYWYgIT0gMCkge1xuICAgICAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBjbnQ7ICsraSkge1xuICAgICAgICAgICAgICAgIHZhciBsbyA9IG9mZnNldC80O1xuICAgICAgICAgICAgICAgIHZhciBzdGFydENocm9tID0gbGFbbG9dO1xuICAgICAgICAgICAgICAgIHZhciBzdGFydEJhc2UgPSBsYVtsbyArIDFdO1xuICAgICAgICAgICAgICAgIHZhciBlbmRDaHJvbSA9IGxhW2xvICsgMl07XG4gICAgICAgICAgICAgICAgdmFyIGVuZEJhc2UgPSBsYVtsbyArIDNdO1xuICAgICAgICAgICAgICAgIHZhciBibG9ja09mZnNldCA9IGJ3Z19yZWFkT2Zmc2V0KGJhLCBvZmZzZXQrMTYpO1xuICAgICAgICAgICAgICAgIHZhciBibG9ja1NpemUgPSBid2dfcmVhZE9mZnNldChiYSwgb2Zmc2V0KzI0KTtcbiAgICAgICAgICAgICAgICBpZiAoKChjaHIgPCAwIHx8IHN0YXJ0Q2hyb20gPCBjaHIpIHx8IChzdGFydENocm9tID09IGNociAmJiBzdGFydEJhc2UgPD0gbWF4KSkgJiZcbiAgICAgICAgICAgICAgICAgICAgKChjaHIgPCAwIHx8IGVuZENocm9tICAgPiBjaHIpIHx8IChlbmRDaHJvbSA9PSBjaHIgJiYgZW5kQmFzZSA+PSBtaW4pKSlcbiAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgIGJsb2Nrc1RvRmV0Y2gucHVzaCh7b2Zmc2V0OiBibG9ja09mZnNldCwgc2l6ZTogYmxvY2tTaXplfSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIG9mZnNldCArPSAzMjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIHZhciByZWN1ck9mZnNldHMgPSBbXTtcbiAgICAgICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgY250OyArK2kpIHtcbiAgICAgICAgICAgICAgICB2YXIgbG8gPSBvZmZzZXQvNDtcbiAgICAgICAgICAgICAgICB2YXIgc3RhcnRDaHJvbSA9IGxhW2xvXTtcbiAgICAgICAgICAgICAgICB2YXIgc3RhcnRCYXNlID0gbGFbbG8gKyAxXTtcbiAgICAgICAgICAgICAgICB2YXIgZW5kQ2hyb20gPSBsYVtsbyArIDJdO1xuICAgICAgICAgICAgICAgIHZhciBlbmRCYXNlID0gbGFbbG8gKyAzXTtcbiAgICAgICAgICAgICAgICB2YXIgYmxvY2tPZmZzZXQgPSBid2dfcmVhZE9mZnNldChiYSwgb2Zmc2V0KzE2KTtcbiAgICAgICAgICAgICAgICBpZiAoKGNociA8IDAgfHwgc3RhcnRDaHJvbSA8IGNociB8fCAoc3RhcnRDaHJvbSA9PSBjaHIgJiYgc3RhcnRCYXNlIDw9IG1heCkpICYmXG4gICAgICAgICAgICAgICAgICAgIChjaHIgPCAwIHx8IGVuZENocm9tICAgPiBjaHIgfHwgKGVuZENocm9tID09IGNociAmJiBlbmRCYXNlID49IG1pbikpKVxuICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgcmVjdXJPZmZzZXRzLnB1c2goYmxvY2tPZmZzZXQpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBvZmZzZXQgKz0gMjQ7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAocmVjdXJPZmZzZXRzLmxlbmd0aCA+IDApIHtcbiAgICAgICAgICAgICAgICBjaXJGb2JSZWN1cihyZWN1ck9mZnNldHMsIGxldmVsICsgMSk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9O1xuXG4gICAgY2lyRm9iUmVjdXIoW3RoaXNCLmNpclRyZWVPZmZzZXQgKyA0OF0sIDEpO1xufVxuXG5cbkJpZ1dpZ1ZpZXcucHJvdG90eXBlLmZldGNoRmVhdHVyZXMgPSBmdW5jdGlvbihmaWx0ZXIsIGJsb2Nrc1RvRmV0Y2gsIGNhbGxiYWNrKSB7XG4gICAgdmFyIHRoaXNCID0gdGhpcztcblxuICAgIGJsb2Nrc1RvRmV0Y2guc29ydChmdW5jdGlvbihiMCwgYjEpIHtcbiAgICAgICAgcmV0dXJuIChiMC5vZmZzZXR8MCkgLSAoYjEub2Zmc2V0fDApO1xuICAgIH0pO1xuXG4gICAgaWYgKGJsb2Nrc1RvRmV0Y2gubGVuZ3RoID09IDApIHtcbiAgICAgICAgY2FsbGJhY2soW10pO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHZhciBmZWF0dXJlcyA9IFtdO1xuICAgICAgICB2YXIgY3JlYXRlRmVhdHVyZSA9IGZ1bmN0aW9uKGNociwgZm1pbiwgZm1heCwgb3B0cykge1xuICAgICAgICAgICAgaWYgKCFvcHRzKSB7XG4gICAgICAgICAgICAgICAgb3B0cyA9IHt9O1xuICAgICAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgICAgIHZhciBmID0gbmV3IERBU0ZlYXR1cmUoKTtcbiAgICAgICAgICAgIGYuX2Nocm9tSWQgPSBjaHI7XG4gICAgICAgICAgICBmLnNlZ21lbnQgPSB0aGlzQi5id2cuaWRzVG9DaHJvbXNbY2hyXTtcbiAgICAgICAgICAgIGYubWluID0gZm1pbjtcbiAgICAgICAgICAgIGYubWF4ID0gZm1heDtcbiAgICAgICAgICAgIGYudHlwZSA9ICdiaWd3aWcnO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICBmb3IgKHZhciBrIGluIG9wdHMpIHtcbiAgICAgICAgICAgICAgICBmW2tdID0gb3B0c1trXTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIFxuICAgICAgICAgICAgZmVhdHVyZXMucHVzaChmKTtcbiAgICAgICAgfTtcblxuICAgICAgICB2YXIgdHJhbXAgPSBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGlmIChibG9ja3NUb0ZldGNoLmxlbmd0aCA9PSAwKSB7XG4gICAgICAgICAgICAgICAgdmFyIGFmdGVyQldHID0gRGF0ZS5ub3coKTtcbiAgICAgICAgICAgICAgICAvLyBkbG9nKCdCV0cgZmV0Y2ggdG9vayAnICsgKGFmdGVyQldHIC0gYmVmb3JlQldHKSArICdtcycpO1xuICAgICAgICAgICAgICAgIGNhbGxiYWNrKGZlYXR1cmVzKTtcbiAgICAgICAgICAgICAgICByZXR1cm47ICAvLyBqdXN0IGluIGNhc2UuLi5cbiAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgdmFyIGJsb2NrID0gYmxvY2tzVG9GZXRjaFswXTtcbiAgICAgICAgICAgICAgICBpZiAoYmxvY2suZGF0YSkge1xuICAgICAgICAgICAgICAgICAgICB0aGlzQi5wYXJzZUZlYXR1cmVzKGJsb2NrLmRhdGEsIGNyZWF0ZUZlYXR1cmUsIGZpbHRlcik7XG4gICAgICAgICAgICAgICAgICAgIGJsb2Nrc1RvRmV0Y2guc3BsaWNlKDAsIDEpO1xuICAgICAgICAgICAgICAgICAgICB0cmFtcCgpO1xuICAgICAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciBmZXRjaFN0YXJ0ID0gYmxvY2sub2Zmc2V0O1xuICAgICAgICAgICAgICAgICAgICB2YXIgZmV0Y2hTaXplID0gYmxvY2suc2l6ZTtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGJpID0gMTtcbiAgICAgICAgICAgICAgICAgICAgd2hpbGUgKGJpIDwgYmxvY2tzVG9GZXRjaC5sZW5ndGggJiYgYmxvY2tzVG9GZXRjaFtiaV0ub2Zmc2V0ID09IChmZXRjaFN0YXJ0ICsgZmV0Y2hTaXplKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgZmV0Y2hTaXplICs9IGJsb2Nrc1RvRmV0Y2hbYmldLnNpemU7XG4gICAgICAgICAgICAgICAgICAgICAgICArK2JpO1xuICAgICAgICAgICAgICAgICAgICB9XG5cbiAgICAgICAgICAgICAgICAgICAgdGhpc0IuYndnLmRhdGEuc2xpY2UoZmV0Y2hTdGFydCwgZmV0Y2hTaXplKS5mZXRjaChmdW5jdGlvbihyZXN1bHQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciBvZmZzZXQgPSAwO1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGJpID0gMDtcbiAgICAgICAgICAgICAgICAgICAgICAgIHdoaWxlIChvZmZzZXQgPCBmZXRjaFNpemUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YXIgZmIgPSBibG9ja3NUb0ZldGNoW2JpXTtcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciBkYXRhO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0aGlzQi5id2cudW5jb21wcmVzc0J1ZlNpemUgPiAwKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGRhdGEgPSBqc3psaWJfaW5mbGF0ZV9idWZmZXIocmVzdWx0LCBvZmZzZXQgKyAyLCBmYi5zaXplIC0gMik7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFyIHRtcCA9IG5ldyBVaW50OEFycmF5KGZiLnNpemUpOyAgICAvLyBGSVhNRSBpcyB0aGlzIHJlYWxseSB0aGUgYmVzdCB3ZSBjYW4gZG8/XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGFycmF5Q29weShuZXcgVWludDhBcnJheShyZXN1bHQsIG9mZnNldCwgZmIuc2l6ZSksIDAsIHRtcCwgMCwgZmIuc2l6ZSk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGRhdGEgPSB0bXAuYnVmZmVyO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBmYi5kYXRhID0gZGF0YTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBvZmZzZXQgKz0gZmIuc2l6ZTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICArK2JpO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgdHJhbXAoKTtcbiAgICAgICAgICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIHRyYW1wKCk7XG4gICAgfVxufVxuXG5CaWdXaWdWaWV3LnByb3RvdHlwZS5wYXJzZUZlYXR1cmVzID0gZnVuY3Rpb24oZGF0YSwgY3JlYXRlRmVhdHVyZSwgZmlsdGVyKSB7XG4gICAgdmFyIGJhID0gbmV3IFVpbnQ4QXJyYXkoZGF0YSk7XG5cbiAgICBpZiAodGhpcy5pc1N1bW1hcnkpIHtcbiAgICAgICAgdmFyIHNhID0gbmV3IEludDE2QXJyYXkoZGF0YSk7XG4gICAgICAgIHZhciBsYSA9IG5ldyBJbnQzMkFycmF5KGRhdGEpO1xuICAgICAgICB2YXIgZmEgPSBuZXcgRmxvYXQzMkFycmF5KGRhdGEpO1xuXG4gICAgICAgIHZhciBpdGVtQ291bnQgPSBkYXRhLmJ5dGVMZW5ndGgvMzI7XG4gICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgaXRlbUNvdW50OyArK2kpIHtcbiAgICAgICAgICAgIHZhciBjaHJvbUlkID0gICBsYVsoaSo4KV07XG4gICAgICAgICAgICB2YXIgc3RhcnQgPSAgICAgbGFbKGkqOCkrMV07XG4gICAgICAgICAgICB2YXIgZW5kID0gICAgICAgbGFbKGkqOCkrMl07XG4gICAgICAgICAgICB2YXIgdmFsaWRDbnQgPSAgbGFbKGkqOCkrM107XG4gICAgICAgICAgICB2YXIgbWluVmFsICAgID0gZmFbKGkqOCkrNF07XG4gICAgICAgICAgICB2YXIgbWF4VmFsICAgID0gZmFbKGkqOCkrNV07XG4gICAgICAgICAgICB2YXIgc3VtRGF0YSAgID0gZmFbKGkqOCkrNl07XG4gICAgICAgICAgICB2YXIgc3VtU3FEYXRhID0gZmFbKGkqOCkrN107XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmIChmaWx0ZXIoY2hyb21JZCwgc3RhcnQgKyAxLCBlbmQpKSB7XG4gICAgICAgICAgICAgICAgdmFyIHN1bW1hcnlPcHRzID0ge3R5cGU6ICdiaWd3aWcnLCBzY29yZTogc3VtRGF0YS92YWxpZENudCwgbWF4U2NvcmU6IG1heFZhbH07XG4gICAgICAgICAgICAgICAgaWYgKHRoaXMuYndnLnR5cGUgPT0gJ2JpZ2JlZCcpIHtcbiAgICAgICAgICAgICAgICAgICAgc3VtbWFyeU9wdHMudHlwZSA9ICdkZW5zaXR5JztcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgY3JlYXRlRmVhdHVyZShjaHJvbUlkLCBzdGFydCArIDEsIGVuZCwgc3VtbWFyeU9wdHMpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfSBlbHNlIGlmICh0aGlzLmJ3Zy50eXBlID09ICdiaWd3aWcnKSB7XG4gICAgICAgIHZhciBzYSA9IG5ldyBJbnQxNkFycmF5KGRhdGEpO1xuICAgICAgICB2YXIgbGEgPSBuZXcgSW50MzJBcnJheShkYXRhKTtcbiAgICAgICAgdmFyIGZhID0gbmV3IEZsb2F0MzJBcnJheShkYXRhKTtcblxuICAgICAgICB2YXIgY2hyb21JZCA9IGxhWzBdO1xuICAgICAgICB2YXIgYmxvY2tTdGFydCA9IGxhWzFdO1xuICAgICAgICB2YXIgYmxvY2tFbmQgPSBsYVsyXTtcbiAgICAgICAgdmFyIGl0ZW1TdGVwID0gbGFbM107XG4gICAgICAgIHZhciBpdGVtU3BhbiA9IGxhWzRdO1xuICAgICAgICB2YXIgYmxvY2tUeXBlID0gYmFbMjBdO1xuICAgICAgICB2YXIgaXRlbUNvdW50ID0gc2FbMTFdO1xuICAgICAgICBcbiAgICAgICAgaWYgKGJsb2NrVHlwZSA9PSBCSUdfV0lHX1RZUEVfRlNURVApIHtcbiAgICAgICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgaXRlbUNvdW50OyArK2kpIHtcbiAgICAgICAgICAgICAgICB2YXIgc2NvcmUgPSBmYVtpICsgNl07XG4gICAgICAgICAgICAgICAgdmFyIGZtaW4gPSBibG9ja1N0YXJ0ICsgKGkqaXRlbVN0ZXApICsgMSwgZm1heCA9IGJsb2NrU3RhcnQgKyAoaSppdGVtU3RlcCkgKyBpdGVtU3BhbjtcbiAgICAgICAgICAgICAgICBpZiAoZmlsdGVyKGNocm9tSWQsIGZtaW4sIGZtYXgpKVxuICAgICAgICAgICAgICAgICAgICBjcmVhdGVGZWF0dXJlKGNocm9tSWQsIGZtaW4sIGZtYXgsIHtzY29yZTogc2NvcmV9KTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSBlbHNlIGlmIChibG9ja1R5cGUgPT0gQklHX1dJR19UWVBFX1ZTVEVQKSB7XG4gICAgICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IGl0ZW1Db3VudDsgKytpKSB7XG4gICAgICAgICAgICAgICAgdmFyIHN0YXJ0ID0gbGFbKGkqMikgKyA2XSArIDE7XG4gICAgICAgICAgICAgICAgdmFyIGVuZCA9IHN0YXJ0ICsgaXRlbVNwYW4gLSAxO1xuICAgICAgICAgICAgICAgIHZhciBzY29yZSA9IGZhWyhpKjIpICsgN107XG4gICAgICAgICAgICAgICAgaWYgKGZpbHRlcihjaHJvbUlkLCBzdGFydCwgZW5kKSlcbiAgICAgICAgICAgICAgICAgICAgY3JlYXRlRmVhdHVyZShjaHJvbUlkLCBzdGFydCwgZW5kLCB7c2NvcmU6IHNjb3JlfSk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0gZWxzZSBpZiAoYmxvY2tUeXBlID09IEJJR19XSUdfVFlQRV9HUkFQSCkge1xuICAgICAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBpdGVtQ291bnQ7ICsraSkge1xuICAgICAgICAgICAgICAgIHZhciBzdGFydCA9IGxhWyhpKjMpICsgNl0gKyAxO1xuICAgICAgICAgICAgICAgIHZhciBlbmQgICA9IGxhWyhpKjMpICsgN107XG4gICAgICAgICAgICAgICAgdmFyIHNjb3JlID0gZmFbKGkqMykgKyA4XTtcbiAgICAgICAgICAgICAgICBpZiAoc3RhcnQgPiBlbmQpIHtcbiAgICAgICAgICAgICAgICAgICAgc3RhcnQgPSBlbmQ7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGlmIChmaWx0ZXIoY2hyb21JZCwgc3RhcnQsIGVuZCkpXG4gICAgICAgICAgICAgICAgICAgIGNyZWF0ZUZlYXR1cmUoY2hyb21JZCwgc3RhcnQsIGVuZCwge3Njb3JlOiBzY29yZX0pO1xuICAgICAgICAgICAgfVxuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgY29uc29sZS5sb2coJ0N1cnJlbnRseSBub3QgaGFuZGxpbmcgYndnVHlwZT0nICsgYmxvY2tUeXBlKTtcbiAgICAgICAgfVxuICAgIH0gZWxzZSBpZiAodGhpcy5id2cudHlwZSA9PSAnYmlnYmVkJykge1xuICAgICAgICB2YXIgb2Zmc2V0ID0gMDtcbiAgICAgICAgdmFyIGRmYyA9IHRoaXMuYndnLmRlZmluZWRGaWVsZENvdW50O1xuICAgICAgICB2YXIgc2NoZW1hID0gdGhpcy5id2cuc2NoZW1hO1xuXG4gICAgICAgIHdoaWxlIChvZmZzZXQgPCBiYS5sZW5ndGgpIHtcbiAgICAgICAgICAgIHZhciBjaHJvbUlkID0gKGJhW29mZnNldCszXTw8MjQpIHwgKGJhW29mZnNldCsyXTw8MTYpIHwgKGJhW29mZnNldCsxXTw8OCkgfCAoYmFbb2Zmc2V0KzBdKTtcbiAgICAgICAgICAgIHZhciBzdGFydCA9IChiYVtvZmZzZXQrN108PDI0KSB8IChiYVtvZmZzZXQrNl08PDE2KSB8IChiYVtvZmZzZXQrNV08PDgpIHwgKGJhW29mZnNldCs0XSk7XG4gICAgICAgICAgICB2YXIgZW5kID0gKGJhW29mZnNldCsxMV08PDI0KSB8IChiYVtvZmZzZXQrMTBdPDwxNikgfCAoYmFbb2Zmc2V0KzldPDw4KSB8IChiYVtvZmZzZXQrOF0pO1xuICAgICAgICAgICAgb2Zmc2V0ICs9IDEyO1xuICAgICAgICAgICAgdmFyIHJlc3QgPSAnJztcbiAgICAgICAgICAgIHdoaWxlICh0cnVlKSB7XG4gICAgICAgICAgICAgICAgdmFyIGNoID0gYmFbb2Zmc2V0KytdO1xuICAgICAgICAgICAgICAgIGlmIChjaCAhPSAwKSB7XG4gICAgICAgICAgICAgICAgICAgIHJlc3QgKz0gU3RyaW5nLmZyb21DaGFyQ29kZShjaCk7XG4gICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICB2YXIgZmVhdHVyZU9wdHMgPSB7fTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgdmFyIGJlZENvbHVtbnM7XG4gICAgICAgICAgICBpZiAocmVzdC5sZW5ndGggPiAwKSB7XG4gICAgICAgICAgICAgICAgYmVkQ29sdW1ucyA9IHJlc3Quc3BsaXQoJ1xcdCcpO1xuICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICBiZWRDb2x1bW5zID0gW107XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoYmVkQ29sdW1ucy5sZW5ndGggPiAwICYmIGRmYyA+IDMpIHtcbiAgICAgICAgICAgICAgICBmZWF0dXJlT3B0cy5sYWJlbCA9IGJlZENvbHVtbnNbMF07XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoYmVkQ29sdW1ucy5sZW5ndGggPiAxICYmIGRmYyA+IDQpIHtcbiAgICAgICAgICAgICAgICB2YXIgc2NvcmUgPSBwYXJzZUludChiZWRDb2x1bW5zWzFdKTtcbiAgICAgICAgICAgICAgICBpZiAoIWlzTmFOKHNjb3JlKSlcbiAgICAgICAgICAgICAgICAgICAgZmVhdHVyZU9wdHMuc2NvcmUgPSBzY29yZTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChiZWRDb2x1bW5zLmxlbmd0aCA+IDIgJiYgZGZjID4gNSkge1xuICAgICAgICAgICAgICAgIGZlYXR1cmVPcHRzLm9yaWVudGF0aW9uID0gYmVkQ29sdW1uc1syXTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChiZWRDb2x1bW5zLmxlbmd0aCA+IDUgJiYgZGZjID4gOCkge1xuICAgICAgICAgICAgICAgIHZhciBjb2xvciA9IGJlZENvbHVtbnNbNV07XG4gICAgICAgICAgICAgICAgaWYgKEJFRF9DT0xPUl9SRUdFWFAudGVzdChjb2xvcikpIHtcbiAgICAgICAgICAgICAgICAgICAgZmVhdHVyZU9wdHMuaXRlbVJnYiA9ICdyZ2IoJyArIGNvbG9yICsgJyknO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgaWYgKGJlZENvbHVtbnMubGVuZ3RoID4gZGZjLTMgJiYgc2NoZW1hKSB7XG4gICAgICAgICAgICAgICAgZm9yICh2YXIgY29sID0gZGZjIC0gMzsgY29sIDwgYmVkQ29sdW1ucy5sZW5ndGg7ICsrY29sKSB7XG4gICAgICAgICAgICAgICAgICAgIGZlYXR1cmVPcHRzW3NjaGVtYS5maWVsZHNbY29sKzNdLm5hbWVdID0gYmVkQ29sdW1uc1tjb2xdO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgaWYgKGZpbHRlcihjaHJvbUlkLCBzdGFydCArIDEsIGVuZCwgYmVkQ29sdW1ucykpIHtcbiAgICAgICAgICAgICAgICBpZiAoZGZjIDwgMTIpIHtcbiAgICAgICAgICAgICAgICAgICAgY3JlYXRlRmVhdHVyZShjaHJvbUlkLCBzdGFydCArIDEsIGVuZCwgZmVhdHVyZU9wdHMpO1xuICAgICAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciB0aGlja1N0YXJ0ID0gYmVkQ29sdW1uc1szXXwwO1xuICAgICAgICAgICAgICAgICAgICB2YXIgdGhpY2tFbmQgICA9IGJlZENvbHVtbnNbNF18MDtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGJsb2NrQ291bnQgPSBiZWRDb2x1bW5zWzZdfDA7XG4gICAgICAgICAgICAgICAgICAgIHZhciBibG9ja1NpemVzID0gYmVkQ29sdW1uc1s3XS5zcGxpdCgnLCcpO1xuICAgICAgICAgICAgICAgICAgICB2YXIgYmxvY2tTdGFydHMgPSBiZWRDb2x1bW5zWzhdLnNwbGl0KCcsJyk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICBmZWF0dXJlT3B0cy50eXBlID0gJ3RyYW5zY3JpcHQnXG4gICAgICAgICAgICAgICAgICAgIHZhciBncnAgPSBuZXcgREFTR3JvdXAoKTtcbiAgICAgICAgICAgICAgICAgICAgZm9yICh2YXIgayBpbiBmZWF0dXJlT3B0cykge1xuICAgICAgICAgICAgICAgICAgICAgICAgZ3JwW2tdID0gZmVhdHVyZU9wdHNba107XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZ3JwLmlkID0gYmVkQ29sdW1uc1swXTtcbiAgICAgICAgICAgICAgICAgICAgZ3JwLnNlZ21lbnQgPSB0aGlzLmJ3Zy5pZHNUb0Nocm9tc1tjaHJvbUlkXTtcbiAgICAgICAgICAgICAgICAgICAgZ3JwLm1pbiA9IHN0YXJ0ICsgMTtcbiAgICAgICAgICAgICAgICAgICAgZ3JwLm1heCA9IGVuZDtcbiAgICAgICAgICAgICAgICAgICAgZ3JwLm5vdGVzID0gW107XG4gICAgICAgICAgICAgICAgICAgIGZlYXR1cmVPcHRzLmdyb3VwcyA9IFtncnBdO1xuXG4gICAgICAgICAgICAgICAgICAgIGlmIChiZWRDb2x1bW5zLmxlbmd0aCA+IDkpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciBnZW5lSWQgPSBiZWRDb2x1bW5zWzldO1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGdlbmVOYW1lID0gZ2VuZUlkO1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKGJlZENvbHVtbnMubGVuZ3RoID4gMTApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBnZW5lTmFtZSA9IGJlZENvbHVtbnNbMTBdO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGdnID0gc2hhbGxvd0NvcHkoZ3JwKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGdnLmlkID0gZ2VuZUlkO1xuICAgICAgICAgICAgICAgICAgICAgICAgZ2cubGFiZWwgPSBnZW5lTmFtZTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGdnLnR5cGUgPSAnZ2VuZSc7XG4gICAgICAgICAgICAgICAgICAgICAgICBmZWF0dXJlT3B0cy5ncm91cHMucHVzaChnZyk7XG4gICAgICAgICAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgICAgICAgICB2YXIgc3Bhbkxpc3QgPSBbXTtcbiAgICAgICAgICAgICAgICAgICAgZm9yICh2YXIgYiA9IDA7IGIgPCBibG9ja0NvdW50OyArK2IpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciBibWluID0gKGJsb2NrU3RhcnRzW2JdfDApICsgc3RhcnQ7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgYm1heCA9IGJtaW4gKyAoYmxvY2tTaXplc1tiXXwwKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciBzcGFuID0gbmV3IFJhbmdlKGJtaW4sIGJtYXgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgc3Bhbkxpc3QucHVzaChzcGFuKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB2YXIgc3BhbnMgPSB1bmlvbihzcGFuTGlzdCk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICB2YXIgdHNMaXN0ID0gc3BhbnMucmFuZ2VzKCk7XG4gICAgICAgICAgICAgICAgICAgIGZvciAodmFyIHMgPSAwOyBzIDwgdHNMaXN0Lmxlbmd0aDsgKytzKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgdHMgPSB0c0xpc3Rbc107XG4gICAgICAgICAgICAgICAgICAgICAgICBjcmVhdGVGZWF0dXJlKGNocm9tSWQsIHRzLm1pbigpICsgMSwgdHMubWF4KCksIGZlYXR1cmVPcHRzKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICAgICAgICAgIGlmICh0aGlja0VuZCA+IHRoaWNrU3RhcnQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciB0bCA9IGludGVyc2VjdGlvbihzcGFucywgbmV3IFJhbmdlKHRoaWNrU3RhcnQsIHRoaWNrRW5kKSk7XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAodGwpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBmZWF0dXJlT3B0cy50eXBlID0gJ3RyYW5zbGF0aW9uJztcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YXIgdGxMaXN0ID0gdGwucmFuZ2VzKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgZm9yICh2YXIgcyA9IDA7IHMgPCB0bExpc3QubGVuZ3RoOyArK3MpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFyIHRzID0gdGxMaXN0W3NdO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjcmVhdGVGZWF0dXJlKGNocm9tSWQsIHRzLm1pbigpICsgMSwgdHMubWF4KCksIGZlYXR1cmVPcHRzKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9IGVsc2Uge1xuICAgICAgICBkbG9nKFwiRG9uJ3Qga25vdyB3aGF0IHRvIGRvIHdpdGggXCIgKyB0aGlzLmJ3Zy50eXBlKTtcbiAgICB9XG59XG5cbi8vXG4vLyBuYXN0eSBjdXQvcGFzdGUsIHNob3VsZCByb2xsIGJhY2sgaW4hXG4vL1xuXG5CaWdXaWdWaWV3LnByb3RvdHlwZS5nZXRGaXJzdEFkamFjZW50ID0gZnVuY3Rpb24oY2hyTmFtZSwgcG9zLCBkaXIsIGNhbGxiYWNrKSB7XG4gICAgdmFyIGNociA9IHRoaXMuYndnLmNocm9tc1RvSURzW2Nock5hbWVdO1xuICAgIGlmIChjaHIgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAvLyBOb3QgYW4gZXJyb3IgYmVjYXVzZSBzb21lIC5id2dzIHdvbid0IGhhdmUgZGF0YSBmb3IgYWxsIGNocm9tb3NvbWVzLlxuXG4gICAgICAgIC8vIGRsb2coXCJDb3VsZG4ndCBmaW5kIGNociBcIiArIGNock5hbWUpO1xuICAgICAgICAvLyBkbG9nKCdDaHJvbXM9JyArIG1pbmlKU09OaWZ5KHRoaXMuYndnLmNocm9tc1RvSURzKSk7XG4gICAgICAgIHJldHVybiBjYWxsYmFjayhbXSk7XG4gICAgfSBlbHNlIHtcbiAgICAgICAgdGhpcy5nZXRGaXJzdEFkamFjZW50QnlJZChjaHIsIHBvcywgZGlyLCBjYWxsYmFjayk7XG4gICAgfVxufVxuXG5CaWdXaWdWaWV3LnByb3RvdHlwZS5nZXRGaXJzdEFkamFjZW50QnlJZCA9IGZ1bmN0aW9uKGNociwgcG9zLCBkaXIsIGNhbGxiYWNrKSB7XG4gICAgdmFyIHRoaXNCID0gdGhpcztcbiAgICBpZiAoIXRoaXMuY2lySGVhZGVyKSB7XG4gICAgICAgIHRoaXMuYndnLmRhdGEuc2xpY2UodGhpcy5jaXJUcmVlT2Zmc2V0LCA0OCkuZmV0Y2goZnVuY3Rpb24ocmVzdWx0KSB7XG4gICAgICAgICAgICB0aGlzQi5jaXJIZWFkZXIgPSByZXN1bHQ7XG4gICAgICAgICAgICB2YXIgbGEgPSBuZXcgSW50MzJBcnJheSh0aGlzQi5jaXJIZWFkZXIpO1xuICAgICAgICAgICAgdGhpc0IuY2lyQmxvY2tTaXplID0gbGFbMV07XG4gICAgICAgICAgICB0aGlzQi5nZXRGaXJzdEFkamFjZW50QnlJZChjaHIsIHBvcywgZGlyLCBjYWxsYmFjayk7XG4gICAgICAgIH0pO1xuICAgICAgICByZXR1cm47XG4gICAgfVxuXG4gICAgdmFyIGJsb2NrVG9GZXRjaCA9IG51bGw7XG4gICAgdmFyIGJlc3RCbG9ja0NociA9IC0xO1xuICAgIHZhciBiZXN0QmxvY2tPZmZzZXQgPSAtMTtcblxuICAgIHZhciBvdXRzdGFuZGluZyA9IDA7XG5cbiAgICB2YXIgYmVmb3JlQldHID0gRGF0ZS5ub3coKTtcblxuICAgIHZhciBjaXJGb2JSZWN1ciA9IGZ1bmN0aW9uKG9mZnNldCwgbGV2ZWwpIHtcbiAgICAgICAgb3V0c3RhbmRpbmcgKz0gb2Zmc2V0Lmxlbmd0aDtcblxuICAgICAgICB2YXIgbWF4Q2lyQmxvY2tTcGFuID0gNCArICAodGhpc0IuY2lyQmxvY2tTaXplICogMzIpOyAgIC8vIFVwcGVyIGJvdW5kIG9uIHNpemUsIGJhc2VkIG9uIGEgY29tcGxldGVseSBmdWxsIGxlYWYgbm9kZS5cbiAgICAgICAgdmFyIHNwYW5zO1xuICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IG9mZnNldC5sZW5ndGg7ICsraSkge1xuICAgICAgICAgICAgdmFyIGJsb2NrU3BhbiA9IG5ldyBSYW5nZShvZmZzZXRbaV0sIG9mZnNldFtpXSArIG1heENpckJsb2NrU3Bhbik7XG4gICAgICAgICAgICBzcGFucyA9IHNwYW5zID8gdW5pb24oc3BhbnMsIGJsb2NrU3BhbikgOiBibG9ja1NwYW47XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciBmZXRjaFJhbmdlcyA9IHNwYW5zLnJhbmdlcygpO1xuICAgICAgICBmb3IgKHZhciByID0gMDsgciA8IGZldGNoUmFuZ2VzLmxlbmd0aDsgKytyKSB7XG4gICAgICAgICAgICB2YXIgZnIgPSBmZXRjaFJhbmdlc1tyXTtcbiAgICAgICAgICAgIGNpckZvYlN0YXJ0RmV0Y2gob2Zmc2V0LCBmciwgbGV2ZWwpO1xuICAgICAgICB9XG4gICAgfVxuXG4gICAgdmFyIGNpckZvYlN0YXJ0RmV0Y2ggPSBmdW5jdGlvbihvZmZzZXQsIGZyLCBsZXZlbCwgYXR0ZW1wdHMpIHtcbiAgICAgICAgdmFyIGxlbmd0aCA9IGZyLm1heCgpIC0gZnIubWluKCk7XG4gICAgICAgIHRoaXNCLmJ3Zy5kYXRhLnNsaWNlKGZyLm1pbigpLCBmci5tYXgoKSAtIGZyLm1pbigpKS5mZXRjaChmdW5jdGlvbihyZXN1bHRCdWZmZXIpIHtcbiAgICAgICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgb2Zmc2V0Lmxlbmd0aDsgKytpKSB7XG4gICAgICAgICAgICAgICAgaWYgKGZyLmNvbnRhaW5zKG9mZnNldFtpXSkpIHtcbiAgICAgICAgICAgICAgICAgICAgY2lyRm9iUmVjdXIyKHJlc3VsdEJ1ZmZlciwgb2Zmc2V0W2ldIC0gZnIubWluKCksIGxldmVsKTtcbiAgICAgICAgICAgICAgICAgICAgLS1vdXRzdGFuZGluZztcbiAgICAgICAgICAgICAgICAgICAgaWYgKG91dHN0YW5kaW5nID09IDApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICghYmxvY2tUb0ZldGNoKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKGRpciA+IDAgJiYgKGNociAhPSAwIHx8IHBvcyA+IDApKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiB0aGlzQi5nZXRGaXJzdEFkamFjZW50QnlJZCgwLCAwLCBkaXIsIGNhbGxiYWNrKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9IGVsc2UgaWYgKGRpciA8IDAgJiYgKGNociAhPSB0aGlzQi5id2cubWF4SUQgfHwgcG9zIDwgMTAwMDAwMDAwMCkpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHRoaXNCLmdldEZpcnN0QWRqYWNlbnRCeUlkKHRoaXNCLmJ3Zy5tYXhJRCwgMTAwMDAwMDAwMCwgZGlyLCBjYWxsYmFjayk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhbXSk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG5cbiAgICAgICAgICAgICAgICAgICAgICAgIHRoaXNCLmZldGNoRmVhdHVyZXMoZnVuY3Rpb24oY2hyeCwgZm1pbiwgZm1heCwgdG9rcykge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiAoZGlyIDwgMCAmJiAoY2hyeCA8IGNociB8fCBmbWF4IDwgcG9zKSkgfHwgKGRpciA+IDAgJiYgKGNocnggPiBjaHIgfHwgZm1pbiA+IHBvcykpO1xuICAgICAgICAgICAgICAgICAgICAgICAgfSwgW2Jsb2NrVG9GZXRjaF0sIGZ1bmN0aW9uKGZlYXR1cmVzKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGJlc3RGZWF0dXJlID0gbnVsbDtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YXIgYmVzdENociA9IC0xO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciBiZXN0UG9zID0gLTE7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgZm9yICh2YXIgZmkgPSAwOyBmaSA8IGZlYXR1cmVzLmxlbmd0aDsgKytmaSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YXIgZiA9IGZlYXR1cmVzW2ZpXTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGNocnggPSBmLl9jaHJvbUlkLCBmbWluID0gZi5taW4sIGZtYXggPSBmLm1heDtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKGJlc3RGZWF0dXJlID09IG51bGwgfHwgKChkaXIgPCAwKSAmJiAoY2hyeCA+IGJlc3RDaHIgfHwgZm1heCA+IGJlc3RQb3MpKSB8fCAoKGRpciA+IDApICYmIChjaHJ4IDwgYmVzdENociB8fCBmbWluIDwgYmVzdFBvcykpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBiZXN0RmVhdHVyZSA9IGY7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBiZXN0UG9zID0gKGRpciA8IDApID8gZm1heCA6IGZtaW47XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBiZXN0Q2hyID0gY2hyeDtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmIChiZXN0RmVhdHVyZSAhPSBudWxsKSBcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKFtiZXN0RmVhdHVyZV0pO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGVsc2VcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKFtdKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICB2YXIgY2lyRm9iUmVjdXIyID0gZnVuY3Rpb24oY2lyQmxvY2tEYXRhLCBvZmZzZXQsIGxldmVsKSB7XG4gICAgICAgIHZhciBiYSA9IG5ldyBVaW50OEFycmF5KGNpckJsb2NrRGF0YSk7XG4gICAgICAgIHZhciBzYSA9IG5ldyBJbnQxNkFycmF5KGNpckJsb2NrRGF0YSk7XG4gICAgICAgIHZhciBsYSA9IG5ldyBJbnQzMkFycmF5KGNpckJsb2NrRGF0YSk7XG5cbiAgICAgICAgdmFyIGlzTGVhZiA9IGJhW29mZnNldF07XG4gICAgICAgIHZhciBjbnQgPSBzYVtvZmZzZXQvMiArIDFdO1xuICAgICAgICBvZmZzZXQgKz0gNDtcblxuICAgICAgICBpZiAoaXNMZWFmICE9IDApIHtcbiAgICAgICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgY250OyArK2kpIHtcbiAgICAgICAgICAgICAgICB2YXIgbG8gPSBvZmZzZXQvNDtcbiAgICAgICAgICAgICAgICB2YXIgc3RhcnRDaHJvbSA9IGxhW2xvXTtcbiAgICAgICAgICAgICAgICB2YXIgc3RhcnRCYXNlID0gbGFbbG8gKyAxXTtcbiAgICAgICAgICAgICAgICB2YXIgZW5kQ2hyb20gPSBsYVtsbyArIDJdO1xuICAgICAgICAgICAgICAgIHZhciBlbmRCYXNlID0gbGFbbG8gKyAzXTtcbiAgICAgICAgICAgICAgICB2YXIgYmxvY2tPZmZzZXQgPSBid2dfcmVhZE9mZnNldChiYSwgb2Zmc2V0KzE2KTtcbiAgICAgICAgICAgICAgICB2YXIgYmxvY2tTaXplID0gYndnX3JlYWRPZmZzZXQoYmEsIG9mZnNldCsyNCk7XG4gICAgICAgICAgICAgICAgaWYgKChkaXIgPCAwICYmICgoc3RhcnRDaHJvbSA8IGNociB8fCAoc3RhcnRDaHJvbSA9PSBjaHIgJiYgc3RhcnRCYXNlIDw9IHBvcykpKSkgfHxcbiAgICAgICAgICAgICAgICAgICAgKGRpciA+IDAgJiYgKChlbmRDaHJvbSA+IGNociB8fCAoZW5kQ2hyb20gPT0gY2hyICYmIGVuZEJhc2UgPj0gcG9zKSkpKSlcbiAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgIC8vIGNvbnNvbGUubG9nKCdHb3QgYW4gaW50ZXJlc3RpbmcgYmxvY2s6IHN0YXJ0QmFzZT0nICsgc3RhcnRDaHJvbSArICc6JyArIHN0YXJ0QmFzZSArICc7IGVuZEJhc2U9JyArIGVuZENocm9tICsgJzonICsgZW5kQmFzZSArICc7IG9mZnNldD0nICsgYmxvY2tPZmZzZXQgKyAnOyBzaXplPScgKyBibG9ja1NpemUpO1xuICAgICAgICAgICAgICAgICAgICBpZiAoL19yYW5kb20vLmV4ZWModGhpc0IuYndnLmlkc1RvQ2hyb21zW3N0YXJ0Q2hyb21dKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgLy8gZGxvZygnc2tpcHBpbmcgcmFuZG9tOiAnICsgdGhpc0IuYndnLmlkc1RvQ2hyb21zW3N0YXJ0Q2hyb21dKTtcbiAgICAgICAgICAgICAgICAgICAgfSBlbHNlIGlmIChibG9ja1RvRmV0Y2ggPT0gbnVsbCB8fCAoKGRpciA8IDApICYmIChlbmRDaHJvbSA+IGJlc3RCbG9ja0NociB8fCAoZW5kQ2hyb20gPT0gYmVzdEJsb2NrQ2hyICYmIGVuZEJhc2UgPiBiZXN0QmxvY2tPZmZzZXQpKSB8fFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIChkaXIgPiAwKSAmJiAoc3RhcnRDaHJvbSA8IGJlc3RCbG9ja0NociB8fCAoc3RhcnRDaHJvbSA9PSBiZXN0QmxvY2tDaHIgJiYgc3RhcnRCYXNlIDwgYmVzdEJsb2NrT2Zmc2V0KSkpKVxuICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgIGRsb2coJ2Jlc3QgaXM6IHN0YXJ0QmFzZT0nICsgc3RhcnRDaHJvbSArICc6JyArIHN0YXJ0QmFzZSArICc7IGVuZEJhc2U9JyArIGVuZENocm9tICsgJzonICsgZW5kQmFzZSArICc7IG9mZnNldD0nICsgYmxvY2tPZmZzZXQgKyAnOyBzaXplPScgKyBibG9ja1NpemUpO1xuICAgICAgICAgICAgICAgICAgICAgICAgYmxvY2tUb0ZldGNoID0ge29mZnNldDogYmxvY2tPZmZzZXQsIHNpemU6IGJsb2NrU2l6ZX07XG4gICAgICAgICAgICAgICAgICAgICAgICBiZXN0QmxvY2tPZmZzZXQgPSAoZGlyIDwgMCkgPyBlbmRCYXNlIDogc3RhcnRCYXNlO1xuICAgICAgICAgICAgICAgICAgICAgICAgYmVzdEJsb2NrQ2hyID0gKGRpciA8IDApID8gZW5kQ2hyb20gOiBzdGFydENocm9tO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIG9mZnNldCArPSAzMjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIHZhciBiZXN0UmVjdXIgPSAtMTtcbiAgICAgICAgICAgIHZhciBiZXN0UG9zID0gLTE7XG4gICAgICAgICAgICB2YXIgYmVzdENociA9IC0xO1xuICAgICAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBjbnQ7ICsraSkge1xuICAgICAgICAgICAgICAgIHZhciBsbyA9IG9mZnNldC80O1xuICAgICAgICAgICAgICAgIHZhciBzdGFydENocm9tID0gbGFbbG9dO1xuICAgICAgICAgICAgICAgIHZhciBzdGFydEJhc2UgPSBsYVtsbyArIDFdO1xuICAgICAgICAgICAgICAgIHZhciBlbmRDaHJvbSA9IGxhW2xvICsgMl07XG4gICAgICAgICAgICAgICAgdmFyIGVuZEJhc2UgPSBsYVtsbyArIDNdO1xuICAgICAgICAgICAgICAgIHZhciBibG9ja09mZnNldCA9IChsYVtsbyArIDRdPDwzMikgfCAobGFbbG8gKyA1XSk7XG4gICAgICAgICAgICAgICAgaWYgKChkaXIgPCAwICYmICgoc3RhcnRDaHJvbSA8IGNociB8fCAoc3RhcnRDaHJvbSA9PSBjaHIgJiYgc3RhcnRCYXNlIDw9IHBvcykpICYmXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAoZW5kQ2hyb20gICA+PSBjaHIpKSkgfHxcbiAgICAgICAgICAgICAgICAgICAgIChkaXIgPiAwICYmICgoZW5kQ2hyb20gPiBjaHIgfHwgKGVuZENocm9tID09IGNociAmJiBlbmRCYXNlID49IHBvcykpICYmXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgKHN0YXJ0Q2hyb20gPD0gY2hyKSkpKVxuICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgaWYgKGJlc3RSZWN1ciA8IDAgfHwgZW5kQmFzZSA+IGJlc3RQb3MpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGJlc3RSZWN1ciA9IGJsb2NrT2Zmc2V0O1xuICAgICAgICAgICAgICAgICAgICAgICAgYmVzdFBvcyA9IChkaXIgPCAwKSA/IGVuZEJhc2UgOiBzdGFydEJhc2U7XG4gICAgICAgICAgICAgICAgICAgICAgICBiZXN0Q2hyID0gKGRpciA8IDApID8gZW5kQ2hyb20gOiBzdGFydENocm9tO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIG9mZnNldCArPSAyNDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChiZXN0UmVjdXIgPj0gMCkge1xuICAgICAgICAgICAgICAgIGNpckZvYlJlY3VyKFtiZXN0UmVjdXJdLCBsZXZlbCArIDEpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfTtcbiAgICBcblxuICAgIGNpckZvYlJlY3VyKFt0aGlzQi5jaXJUcmVlT2Zmc2V0ICsgNDhdLCAxKTtcbn1cblxuQmlnV2lnLnByb3RvdHlwZS5yZWFkV2lnRGF0YSA9IGZ1bmN0aW9uKGNock5hbWUsIG1pbiwgbWF4LCBjYWxsYmFjaykge1xuICAgIHRoaXMuZ2V0VW56b29tZWRWaWV3KCkucmVhZFdpZ0RhdGEoY2hyTmFtZSwgbWluLCBtYXgsIGNhbGxiYWNrKTtcbn1cblxuQmlnV2lnLnByb3RvdHlwZS5nZXRVbnpvb21lZFZpZXcgPSBmdW5jdGlvbigpIHtcbiAgICBpZiAoIXRoaXMudW56b29tZWRWaWV3KSB7XG4gICAgICAgIHZhciBjaXJMZW4gPSA0MDAwO1xuICAgICAgICB2YXIgbnpsID0gdGhpcy56b29tTGV2ZWxzWzBdO1xuICAgICAgICBpZiAobnpsKSB7XG4gICAgICAgICAgICBjaXJMZW4gPSB0aGlzLnpvb21MZXZlbHNbMF0uZGF0YU9mZnNldCAtIHRoaXMudW56b29tZWRJbmRleE9mZnNldDtcbiAgICAgICAgfVxuICAgICAgICB0aGlzLnVuem9vbWVkVmlldyA9IG5ldyBCaWdXaWdWaWV3KHRoaXMsIHRoaXMudW56b29tZWRJbmRleE9mZnNldCwgY2lyTGVuLCBmYWxzZSk7XG4gICAgfVxuICAgIHJldHVybiB0aGlzLnVuem9vbWVkVmlldztcbn1cblxuQmlnV2lnLnByb3RvdHlwZS5nZXRab29tZWRWaWV3ID0gZnVuY3Rpb24oeikge1xuICAgIHZhciB6aCA9IHRoaXMuem9vbUxldmVsc1t6XTtcbiAgICBpZiAoIXpoLnZpZXcpIHtcbiAgICAgICAgemgudmlldyA9IG5ldyBCaWdXaWdWaWV3KHRoaXMsIHpoLmluZGV4T2Zmc2V0LCAvKiB0aGlzLnpvb21MZXZlbHNbeiArIDFdLmRhdGFPZmZzZXQgLSB6aC5pbmRleE9mZnNldCAqLyA0MDAwLCB0cnVlKTtcbiAgICB9XG4gICAgcmV0dXJuIHpoLnZpZXc7XG59XG5cbmZ1bmN0aW9uIG1ha2VCd2coZGF0YSwgY2FsbGJhY2ssIG5hbWUpIHtcbiAgICB2YXIgYndnID0gbmV3IEJpZ1dpZygpO1xuICAgIGJ3Zy5kYXRhID0gZGF0YTtcbiAgICBid2cubmFtZSA9IG5hbWU7XG4gICAgYndnLmRhdGEuc2xpY2UoMCwgNTEyKS5zYWx0ZWQoKS5mZXRjaChmdW5jdGlvbihyZXN1bHQpIHtcbiAgICAgICAgaWYgKCFyZXN1bHQpIHtcbiAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhudWxsLCBcIkNvdWxkbid0IGZldGNoIGZpbGVcIik7XG4gICAgICAgIH1cblxuICAgICAgICB2YXIgaGVhZGVyID0gcmVzdWx0O1xuICAgICAgICB2YXIgYmEgPSBuZXcgVWludDhBcnJheShoZWFkZXIpO1xuICAgICAgICB2YXIgc2EgPSBuZXcgSW50MTZBcnJheShoZWFkZXIpO1xuICAgICAgICB2YXIgbGEgPSBuZXcgSW50MzJBcnJheShoZWFkZXIpO1xuICAgICAgICB2YXIgbWFnaWMgPSBiYVswXSArIChNMSAqIGJhWzFdKSArIChNMiAqIGJhWzJdKSArIChNMyAqIGJhWzNdKTtcbiAgICAgICAgaWYgKG1hZ2ljID09IEJJR19XSUdfTUFHSUMpIHtcbiAgICAgICAgICAgIGJ3Zy50eXBlID0gJ2JpZ3dpZyc7XG4gICAgICAgIH0gZWxzZSBpZiAobWFnaWMgPT0gQklHX0JFRF9NQUdJQykge1xuICAgICAgICAgICAgYndnLnR5cGUgPSAnYmlnYmVkJztcbiAgICAgICAgfSBlbHNlIGlmIChtYWdpYyA9PSBCSUdfV0lHX01BR0lDX0JFIHx8IG1hZ2ljID09IEJJR19CRURfTUFHSUNfQkUpIHtcbiAgICAgICAgICAgIGNhbGxiYWNrKG51bGwsIFwiQ3VycmVudGx5IGRvbid0IHN1cHBvcnQgYmlnLWVuZGlhbiBCQkkgZmlsZXNcIik7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBjYWxsYmFjayhudWxsLCBcIk5vdCBhIHN1cHBvcnRlZCBmb3JtYXQsIG1hZ2ljPTB4XCIgKyBtYWdpYy50b1N0cmluZygxNikpO1xuICAgICAgICB9XG5cbiAgICAgICAgYndnLnZlcnNpb24gPSBzYVsyXTsgICAgICAgICAgICAgLy8gNFxuICAgICAgICBid2cubnVtWm9vbUxldmVscyA9IHNhWzNdOyAgICAgICAvLyA2XG4gICAgICAgIGJ3Zy5jaHJvbVRyZWVPZmZzZXQgPSBid2dfcmVhZE9mZnNldChiYSwgOCk7XG4gICAgICAgIGJ3Zy51bnpvb21lZERhdGFPZmZzZXQgPSBid2dfcmVhZE9mZnNldChiYSwgMTYpO1xuICAgICAgICBid2cudW56b29tZWRJbmRleE9mZnNldCA9IGJ3Z19yZWFkT2Zmc2V0KGJhLCAyNCk7XG4gICAgICAgIGJ3Zy5maWVsZENvdW50ID0gc2FbMTZdOyAgICAgICAgIC8vIDMyXG4gICAgICAgIGJ3Zy5kZWZpbmVkRmllbGRDb3VudCA9IHNhWzE3XTsgIC8vIDM0XG4gICAgICAgIGJ3Zy5hc09mZnNldCA9IGJ3Z19yZWFkT2Zmc2V0KGJhLCAzNik7XG4gICAgICAgIGJ3Zy50b3RhbFN1bW1hcnlPZmZzZXQgPSBid2dfcmVhZE9mZnNldChiYSwgNDQpO1xuICAgICAgICBid2cudW5jb21wcmVzc0J1ZlNpemUgPSBsYVsxM107ICAvLyA1MlxuICAgICAgICBid2cuZXh0SGVhZGVyT2Zmc2V0ID0gYndnX3JlYWRPZmZzZXQoYmEsIDU2KTtcblxuICAgICAgICBid2cuem9vbUxldmVscyA9IFtdO1xuICAgICAgICBmb3IgKHZhciB6bCA9IDA7IHpsIDwgYndnLm51bVpvb21MZXZlbHM7ICsremwpIHtcbiAgICAgICAgICAgIHZhciB6bFJlZHVjdGlvbiA9IGxhW3psKjYgKyAxNl1cbiAgICAgICAgICAgIHZhciB6bERhdGEgPSBid2dfcmVhZE9mZnNldChiYSwgemwqMjQgKyA3Mik7XG4gICAgICAgICAgICB2YXIgemxJbmRleCA9IGJ3Z19yZWFkT2Zmc2V0KGJhLCB6bCoyNCArIDgwKTtcbiAgICAgICAgICAgIGJ3Zy56b29tTGV2ZWxzLnB1c2goe3JlZHVjdGlvbjogemxSZWR1Y3Rpb24sIGRhdGFPZmZzZXQ6IHpsRGF0YSwgaW5kZXhPZmZzZXQ6IHpsSW5kZXh9KTtcbiAgICAgICAgfVxuXG4gICAgICAgIGJ3Zy5yZWFkQ2hyb21UcmVlKGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgYndnLmdldEF1dG9TUUwoZnVuY3Rpb24oYXMpIHtcbiAgICAgICAgICAgICAgICBid2cuc2NoZW1hID0gYXM7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKGJ3Zyk7XG4gICAgICAgICAgICB9KTtcbiAgICAgICAgfSk7XG4gICAgfSk7XG59XG5cblxuQmlnV2lnLnByb3RvdHlwZS5fdHNGZXRjaCA9IGZ1bmN0aW9uKHpvb20sIGNociwgbWluLCBtYXgsIGNhbGxiYWNrKSB7XG4gICAgdmFyIGJ3ZyA9IHRoaXM7XG4gICAgaWYgKHpvb20gPj0gdGhpcy56b29tTGV2ZWxzLmxlbmd0aCAtIDEpIHtcbiAgICAgICAgaWYgKCF0aGlzLnRvcExldmVsUmVkdWN0aW9uQ2FjaGUpIHtcbiAgICAgICAgICAgIHRoaXMuZ2V0Wm9vbWVkVmlldyh0aGlzLnpvb21MZXZlbHMubGVuZ3RoIC0gMSkucmVhZFdpZ0RhdGFCeUlkKC0xLCAwLCAzMDAwMDAwMDAsIGZ1bmN0aW9uKGZlYXRzKSB7XG4gICAgICAgICAgICAgICAgYndnLnRvcExldmVsUmVkdWN0aW9uQ2FjaGUgPSBmZWF0cztcbiAgICAgICAgICAgICAgICByZXR1cm4gYndnLl90c0ZldGNoKHpvb20sIGNociwgbWluLCBtYXgsIGNhbGxiYWNrKTtcbiAgICAgICAgICAgIH0pO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgdmFyIGYgPSBbXTtcbiAgICAgICAgICAgIHZhciBjID0gdGhpcy50b3BMZXZlbFJlZHVjdGlvbkNhY2hlO1xuICAgICAgICAgICAgZm9yICh2YXIgZmkgPSAwOyBmaSA8IGMubGVuZ3RoOyArK2ZpKSB7XG4gICAgICAgICAgICAgICAgaWYgKGNbZmldLl9jaHJvbUlkID09IGNocikge1xuICAgICAgICAgICAgICAgICAgICBmLnB1c2goY1tmaV0pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhmKTtcbiAgICAgICAgfVxuICAgIH0gZWxzZSB7XG4gICAgICAgIHZhciB2aWV3O1xuICAgICAgICBpZiAoem9vbSA8IDApIHtcbiAgICAgICAgICAgIHZpZXcgPSB0aGlzLmdldFVuem9vbWVkVmlldygpO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgdmlldyA9IHRoaXMuZ2V0Wm9vbWVkVmlldyh6b29tKTtcbiAgICAgICAgfVxuICAgICAgICByZXR1cm4gdmlldy5yZWFkV2lnRGF0YUJ5SWQoY2hyLCBtaW4sIG1heCwgY2FsbGJhY2spO1xuICAgIH1cbn1cblxuQmlnV2lnLnByb3RvdHlwZS50aHJlc2hvbGRTZWFyY2ggPSBmdW5jdGlvbihjaHJOYW1lLCByZWZlcmVuY2VQb2ludCwgZGlyLCB0aHJlc2hvbGQsIGNhbGxiYWNrKSB7XG4gICAgZGlyID0gKGRpcjwwKSA/IC0xIDogMTtcbiAgICB2YXIgYndnID0gdGhpcztcbiAgICB2YXIgaW5pdGlhbENociA9IHRoaXMuY2hyb21zVG9JRHNbY2hyTmFtZV07XG4gICAgdmFyIGNhbmRpZGF0ZXMgPSBbe2Nock9yZDogMCwgY2hyOiBpbml0aWFsQ2hyLCB6b29tOiBid2cuem9vbUxldmVscy5sZW5ndGggLSA0LCBtaW46IDAsIG1heDogMzAwMDAwMDAwLCBmcm9tUmVmOiB0cnVlfV1cbiAgICBmb3IgKHZhciBpID0gMTsgaSA8PSB0aGlzLm1heElEICsgMTsgKytpKSB7XG4gICAgICAgIHZhciBjaHJJZCA9IChpbml0aWFsQ2hyICsgKGRpcippKSkgJSAodGhpcy5tYXhJRCArIDEpO1xuICAgICAgICBpZiAoY2hySWQgPCAwKSBcbiAgICAgICAgICAgIGNocklkICs9ICh0aGlzLm1heElEICsgMSk7XG4gICAgICAgIGNhbmRpZGF0ZXMucHVzaCh7Y2hyT3JkOiBpLCBjaHI6IGNocklkLCB6b29tOiBid2cuem9vbUxldmVscy5sZW5ndGggLSAxLCBtaW46IDAsIG1heDogMzAwMDAwMDAwfSlcbiAgICB9XG4gICAgICAgXG4gICAgZnVuY3Rpb24gZmJUaHJlc2hvbGRTZWFyY2hSZWN1cigpIHtcbiAgICBcdGlmIChjYW5kaWRhdGVzLmxlbmd0aCA9PSAwKSB7XG4gICAgXHQgICAgcmV0dXJuIGNhbGxiYWNrKG51bGwpO1xuICAgIFx0fVxuICAgIFx0Y2FuZGlkYXRlcy5zb3J0KGZ1bmN0aW9uKGMxLCBjMikge1xuICAgIFx0ICAgIHZhciBkID0gYzEuem9vbSAtIGMyLnpvb207XG4gICAgXHQgICAgaWYgKGQgIT0gMClcbiAgICBcdFx0ICAgIHJldHVybiBkO1xuXG4gICAgICAgICAgICBkID0gYzEuY2hyT3JkIC0gYzIuY2hyT3JkO1xuICAgICAgICAgICAgaWYgKGQgIT0gMClcbiAgICAgICAgICAgICAgICByZXR1cm4gZDtcbiAgICBcdCAgICBlbHNlXG4gICAgXHRcdCAgICByZXR1cm4gYzEubWluIC0gYzIubWluICogZGlyO1xuICAgIFx0fSk7XG5cblx0ICAgIHZhciBjYW5kaWRhdGUgPSBjYW5kaWRhdGVzLnNwbGljZSgwLCAxKVswXTtcbiAgICAgICAgYndnLl90c0ZldGNoKGNhbmRpZGF0ZS56b29tLCBjYW5kaWRhdGUuY2hyLCBjYW5kaWRhdGUubWluLCBjYW5kaWRhdGUubWF4LCBmdW5jdGlvbihmZWF0cykge1xuICAgICAgICAgICAgdmFyIHJwID0gZGlyID4gMCA/IDAgOiAzMDAwMDAwMDA7XG4gICAgICAgICAgICBpZiAoY2FuZGlkYXRlLmZyb21SZWYpXG4gICAgICAgICAgICAgICAgcnAgPSByZWZlcmVuY2VQb2ludDtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgZm9yICh2YXIgZmkgPSAwOyBmaSA8IGZlYXRzLmxlbmd0aDsgKytmaSkge1xuICAgIFx0ICAgICAgICB2YXIgZiA9IGZlYXRzW2ZpXTtcbiAgICAgICAgICAgICAgICB2YXIgc2NvcmU7XG4gICAgICAgICAgICAgICAgaWYgKGYubWF4U2NvcmUgIT0gdW5kZWZpbmVkKVxuICAgICAgICAgICAgICAgICAgICBzY29yZSA9IGYubWF4U2NvcmU7XG4gICAgICAgICAgICAgICAgZWxzZVxuICAgICAgICAgICAgICAgICAgICBzY29yZSA9IGYuc2NvcmU7XG5cbiAgICAgICAgICAgICAgICBpZiAoZGlyID4gMCkge1xuICAgIFx0ICAgICAgICAgICAgaWYgKHNjb3JlID4gdGhyZXNob2xkKSB7XG4gICAgICAgIFx0XHQgICAgICAgIGlmIChjYW5kaWRhdGUuem9vbSA8IDApIHtcbiAgICAgICAgXHRcdCAgICAgICAgICAgIGlmIChmLm1pbiA+IHJwKVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gY2FsbGJhY2soZik7XG4gICAgICAgIFx0XHQgICAgICAgIH0gZWxzZSBpZiAoZi5tYXggPiBycCkge1xuICAgICAgICBcdFx0ICAgICAgICAgICAgY2FuZGlkYXRlcy5wdXNoKHtjaHI6IGNhbmRpZGF0ZS5jaHIsIGNock9yZDogY2FuZGlkYXRlLmNock9yZCwgem9vbTogY2FuZGlkYXRlLnpvb20gLSAyLCBtaW46IGYubWluLCBtYXg6IGYubWF4LCBmcm9tUmVmOiBjYW5kaWRhdGUuZnJvbVJlZn0pO1xuICAgICAgICBcdFx0ICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBpZiAoc2NvcmUgPiB0aHJlc2hvbGQpIHtcbiAgICAgICAgICAgIFx0XHQgICAgaWYgKGNhbmRpZGF0ZS56b29tIDwgMCkge1xuICAgICAgICAgICAgICAgIFx0ICAgICAgICBpZiAoZi5tYXggPCBycClcbiAgICAgICAgICAgICAgICBcdFx0XHQgICAgcmV0dXJuIGNhbGxiYWNrKGYpO1xuICAgICAgICAgICAgICAgICAgICAgICAgfSBlbHNlIGlmIChmLm1pbiA8IHJwKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgY2FuZGlkYXRlcy5wdXNoKHtjaHI6IGNhbmRpZGF0ZS5jaHIsIGNock9yZDogY2FuZGlkYXRlLmNock9yZCwgem9vbTogY2FuZGlkYXRlLnpvb20gLSAyLCBtaW46IGYubWluLCBtYXg6IGYubWF4LCBmcm9tUmVmOiBjYW5kaWRhdGUuZnJvbVJlZn0pO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgIFx0ICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICBcdCAgICB9XG4gICAgICAgICAgICBmYlRocmVzaG9sZFNlYXJjaFJlY3VyKCk7XG4gICAgICAgIH0pO1xuICAgIH1cbiAgICBcbiAgICBmYlRocmVzaG9sZFNlYXJjaFJlY3VyKCk7XG59XG5cbkJpZ1dpZy5wcm90b3R5cGUuZ2V0QXV0b1NRTCA9IGZ1bmN0aW9uKGNhbGxiYWNrKSB7XG4gICAgdmFyIHRoaXNCID0gdGhpcztcbiAgICBpZiAoIXRoaXMuYXNPZmZzZXQpXG4gICAgICAgIHJldHVybiBjYWxsYmFjayhudWxsKTtcblxuXG4gICAgdGhpcy5kYXRhLnNsaWNlKHRoaXMuYXNPZmZzZXQsIDIwNDgpLmZldGNoKGZ1bmN0aW9uKHJlc3VsdCkge1xuICAgICAgICB2YXIgYmEgPSBuZXcgVWludDhBcnJheShyZXN1bHQpO1xuICAgICAgICB2YXIgcyA9ICcnO1xuICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IGJhLmxlbmd0aDsgKytpKSB7XG4gICAgICAgICAgICBpZiAoYmFbaV0gPT0gMClcbiAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgIHMgKz0gU3RyaW5nLmZyb21DaGFyQ29kZShiYVtpXSk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIC8qIFxuICAgICAgICAgKiBRdWljayduJ2RpcnR5IGF0dGVtcHQgdG8gcGFyc2UgYXV0b1NxbCBmb3JtYXQuXG4gICAgICAgICAqIFNlZTogaHR0cDovL3d3dy5saW51eGpvdXJuYWwuY29tL2ZpbGVzL2xpbnV4am91cm5hbC5jb20vbGludXhqb3VybmFsL2FydGljbGVzLzA1OS81OTQ5LzU5NDlsMi5odG1sXG4gICAgICAgICAqL1xuXG4gICAgICAgIHZhciBoZWFkZXJfcmUgPSAvKFxcdyspXFxzKyhcXHcrKVxccysoXCIoW15cIl0rKVwiKT9cXHMrXFwoXFxzKi87XG4gICAgICAgIHZhciBmaWVsZF9yZSA9IC8oW1xcd1xcW1xcXV0rKVxccysoXFx3KylcXHMqO1xccyooXCIoW15cIl0rKVwiKT9cXHMqL2c7XG5cbiAgICAgICAgdmFyIGhlYWRlck1hdGNoID0gaGVhZGVyX3JlLmV4ZWMocyk7XG4gICAgICAgIGlmIChoZWFkZXJNYXRjaCkge1xuICAgICAgICAgICAgdmFyIGFzID0ge1xuICAgICAgICAgICAgICAgIGRlY2xUeXBlOiBoZWFkZXJNYXRjaFsxXSxcbiAgICAgICAgICAgICAgICBuYW1lOiBoZWFkZXJNYXRjaFsyXSxcbiAgICAgICAgICAgICAgICBjb21tZW50OiBoZWFkZXJNYXRjaFs0XSxcblxuICAgICAgICAgICAgICAgIGZpZWxkczogW11cbiAgICAgICAgICAgIH07XG5cbiAgICAgICAgICAgIHMgPSBzLnN1YnN0cmluZyhoZWFkZXJNYXRjaFswXSk7XG4gICAgICAgICAgICBmb3IgKHZhciBtID0gZmllbGRfcmUuZXhlYyhzKTsgbSAhPSBudWxsOyBtID0gZmllbGRfcmUuZXhlYyhzKSkge1xuICAgICAgICAgICAgICAgIGFzLmZpZWxkcy5wdXNoKHt0eXBlOiBtWzFdLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiBtWzJdLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb21tZW50OiBtWzRdfSk7XG4gICAgICAgICAgICB9XG5cbiAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhhcyk7XG4gICAgICAgIH1cbiAgICB9KTtcbn1cblxuQmlnV2lnLnByb3RvdHlwZS5nZXRFeHRyYUluZGljZXMgPSBmdW5jdGlvbihjYWxsYmFjaykge1xuICAgIHZhciB0aGlzQiA9IHRoaXM7XG4gICAgaWYgKHRoaXMudmVyc2lvbiA8IDQgfHwgdGhpcy5leHRIZWFkZXJPZmZzZXQgPT0gMCB8fCB0aGlzLnR5cGUgIT0gJ2JpZ2JlZCcpIHtcbiAgICAgICAgcmV0dXJuIGNhbGxiYWNrKG51bGwpO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHRoaXMuZGF0YS5zbGljZSh0aGlzLmV4dEhlYWRlck9mZnNldCwgNjQpLmZldGNoKGZ1bmN0aW9uKHJlc3VsdCkge1xuICAgICAgICAgICAgaWYgKCFyZXN1bHQpIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gY2FsbGJhY2sobnVsbCwgXCJDb3VsZG4ndCBmZXRjaCBleHRlbnNpb24gaGVhZGVyXCIpO1xuICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICB2YXIgYmEgPSBuZXcgVWludDhBcnJheShyZXN1bHQpO1xuICAgICAgICAgICAgdmFyIHNhID0gbmV3IEludDE2QXJyYXkocmVzdWx0KTtcbiAgICAgICAgICAgIHZhciBsYSA9IG5ldyBJbnQzMkFycmF5KHJlc3VsdCk7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBleHRIZWFkZXJTaXplID0gc2FbMF07XG4gICAgICAgICAgICB2YXIgZXh0cmFJbmRleENvdW50ID0gc2FbMV07XG4gICAgICAgICAgICB2YXIgZXh0cmFJbmRleExpc3RPZmZzZXQgPSBid2dfcmVhZE9mZnNldChiYSwgNCk7XG5cbiAgICAgICAgICAgIGlmIChleHRyYUluZGV4Q291bnQgPT0gMCkge1xuICAgICAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhudWxsKTtcbiAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgLy8gRklYTUUgMjBieXRlIHJlY29yZHMgb25seSBtYWtlIHNlbnNlIGZvciBzaW5nbGUtZmllbGQgaW5kaWNlcy5cbiAgICAgICAgICAgIC8vIFJpZ2h0IG5vdywgdGhlc2Ugc2VlbSB0byBiZSB0aGUgb25seSB0aGluZ3MgYXJvdW5kLCBidXQgdGhlIGZvcm1hdFxuICAgICAgICAgICAgLy8gaXMgYWN0dWFsbHkgbW9yZSBnZW5lcmFsLlxuICAgICAgICAgICAgdGhpc0IuZGF0YS5zbGljZShleHRyYUluZGV4TGlzdE9mZnNldCwgZXh0cmFJbmRleENvdW50ICogMjApLmZldGNoKGZ1bmN0aW9uKGVpbCkge1xuICAgICAgICAgICAgICAgIGlmICghZWlsKSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhudWxsLCBcIkNvdWxkbid0IGZldGNoIGluZGV4IGluZm9cIik7XG4gICAgICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICAgICAgdmFyIGJhID0gbmV3IFVpbnQ4QXJyYXkoZWlsKTtcbiAgICAgICAgICAgICAgICB2YXIgc2EgPSBuZXcgSW50MTZBcnJheShlaWwpO1xuICAgICAgICAgICAgICAgIHZhciBsYSA9IG5ldyBJbnQzMkFycmF5KGVpbCk7XG5cbiAgICAgICAgICAgICAgICB2YXIgaW5kaWNlcyA9IFtdO1xuICAgICAgICAgICAgICAgIGZvciAodmFyIGlpID0gMDsgaWkgPCBleHRyYUluZGV4Q291bnQ7ICsraWkpIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGVpVHlwZSA9IHNhW2lpKjEwXTtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGVpRmllbGRDb3VudCA9IHNhW2lpKjEwICsgMV07XG4gICAgICAgICAgICAgICAgICAgIHZhciBlaU9mZnNldCA9IGJ3Z19yZWFkT2Zmc2V0KGJhLCBpaSoyMCArIDQpO1xuICAgICAgICAgICAgICAgICAgICB2YXIgZWlGaWVsZCA9IHNhW2lpKjEwICsgOF1cbiAgICAgICAgICAgICAgICAgICAgdmFyIGluZGV4ID0gbmV3IEJCSUV4dHJhSW5kZXgodGhpc0IsIGVpVHlwZSwgZWlGaWVsZENvdW50LCBlaU9mZnNldCwgZWlGaWVsZCk7XG4gICAgICAgICAgICAgICAgICAgIGluZGljZXMucHVzaChpbmRleCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGNhbGxiYWNrKGluZGljZXMpO1xuICAgICAgICAgICAgfSk7XG4gICAgICAgIH0pO1xuICAgIH1cbn1cblxuZnVuY3Rpb24gQkJJRXh0cmFJbmRleChiYmksIHR5cGUsIGZpZWxkQ291bnQsIG9mZnNldCwgZmllbGQpIHtcbiAgICB0aGlzLmJiaSA9IGJiaTtcbiAgICB0aGlzLnR5cGUgPSB0eXBlO1xuICAgIHRoaXMuZmllbGRDb3VudCA9IGZpZWxkQ291bnQ7XG4gICAgdGhpcy5vZmZzZXQgPSBvZmZzZXQ7XG4gICAgdGhpcy5maWVsZCA9IGZpZWxkO1xufVxuXG5CQklFeHRyYUluZGV4LnByb3RvdHlwZS5sb29rdXAgPSBmdW5jdGlvbihuYW1lLCBjYWxsYmFjaykge1xuICAgIHZhciB0aGlzQiA9IHRoaXM7XG5cbiAgICB0aGlzLmJiaS5kYXRhLnNsaWNlKHRoaXMub2Zmc2V0LCAzMikuZmV0Y2goZnVuY3Rpb24oYnB0KSB7XG4gICAgICAgIHZhciBiYSA9IG5ldyBVaW50OEFycmF5KGJwdCk7XG4gICAgICAgIHZhciBzYSA9IG5ldyBJbnQxNkFycmF5KGJwdCk7XG4gICAgICAgIHZhciBsYSA9IG5ldyBJbnQzMkFycmF5KGJwdCk7XG4gICAgICAgIHZhciBicHRNYWdpYyA9IGxhWzBdO1xuICAgICAgICB2YXIgYmxvY2tTaXplID0gbGFbMV07XG4gICAgICAgIHZhciBrZXlTaXplID0gbGFbMl07XG4gICAgICAgIHZhciB2YWxTaXplID0gbGFbM107XG4gICAgICAgIHZhciBpdGVtQ291bnQgPSBid2dfcmVhZE9mZnNldChiYSwgMTYpO1xuICAgICAgICB2YXIgcm9vdE5vZGVPZmZzZXQgPSAzMjtcblxuICAgICAgICBmdW5jdGlvbiBicHRSZWFkTm9kZShub2RlT2Zmc2V0KSB7XG4gICAgICAgICAgICB0aGlzQi5iYmkuZGF0YS5zbGljZShub2RlT2Zmc2V0LCA0ICsgKGJsb2NrU2l6ZSAqIChrZXlTaXplICsgdmFsU2l6ZSkpKS5mZXRjaChmdW5jdGlvbihub2RlKSB7XG4gICAgICAgICAgICAgICAgdmFyIGJhID0gbmV3IFVpbnQ4QXJyYXkobm9kZSk7XG4gICAgICAgICAgICAgICAgdmFyIHNhID0gbmV3IFVpbnQxNkFycmF5KG5vZGUpO1xuICAgICAgICAgICAgICAgIHZhciBsYSA9IG5ldyBVaW50MzJBcnJheShub2RlKTtcblxuICAgICAgICAgICAgICAgIHZhciBub2RlVHlwZSA9IGJhWzBdO1xuICAgICAgICAgICAgICAgIHZhciBjbnQgPSBzYVsxXTtcblxuICAgICAgICAgICAgICAgIHZhciBvZmZzZXQgPSA0O1xuICAgICAgICAgICAgICAgIGlmIChub2RlVHlwZSA9PSAwKSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciBsYXN0Q2hpbGRPZmZzZXQgPSBudWxsO1xuICAgICAgICAgICAgICAgICAgICBmb3IgKHZhciBuID0gMDsgbiA8IGNudDsgKytuKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIga2V5ID0gJyc7XG4gICAgICAgICAgICAgICAgICAgICAgICBmb3IgKHZhciBraSA9IDA7IGtpIDwga2V5U2l6ZTsgKytraSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciBjaGFyQ29kZSA9IGJhW29mZnNldCsrXTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAoY2hhckNvZGUgIT0gMCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBrZXkgKz0gU3RyaW5nLmZyb21DaGFyQ29kZShjaGFyQ29kZSk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgY2hpbGRPZmZzZXQgPSBid2dfcmVhZE9mZnNldChiYSwgb2Zmc2V0KTtcbiAgICAgICAgICAgICAgICAgICAgICAgIG9mZnNldCArPSA4O1xuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAobmFtZS5sb2NhbGVDb21wYXJlKGtleSkgPCAwICYmIGxhc3RDaGlsZE9mZnNldCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJwdFJlYWROb2RlKGxhc3RDaGlsZE9mZnNldCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgbGFzdENoaWxkT2Zmc2V0ID0gY2hpbGRPZmZzZXQ7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgYnB0UmVhZE5vZGUobGFzdENoaWxkT2Zmc2V0KTtcbiAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBmb3IgKHZhciBuID0gMDsgbiA8IGNudDsgKytuKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIga2V5ID0gJyc7XG4gICAgICAgICAgICAgICAgICAgICAgICBmb3IgKHZhciBraSA9IDA7IGtpIDwga2V5U2l6ZTsgKytraSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciBjaGFyQ29kZSA9IGJhW29mZnNldCsrXTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAoY2hhckNvZGUgIT0gMCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBrZXkgKz0gU3RyaW5nLmZyb21DaGFyQ29kZShjaGFyQ29kZSk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAvLyBTcGVjaWZpYyBmb3IgRUkgY2FzZS5cbiAgICAgICAgICAgICAgICAgICAgICAgIGlmIChrZXkgPT0gbmFtZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciBzdGFydCA9IGJ3Z19yZWFkT2Zmc2V0KGJhLCBvZmZzZXQpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciBsZW5ndGggPSByZWFkSW50KGJhLCBvZmZzZXQgKyA4KTtcblxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiB0aGlzQi5iYmkuZ2V0VW56b29tZWRWaWV3KCkuZmV0Y2hGZWF0dXJlcyhcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgZnVuY3Rpb24oY2hyLCBtaW4sIG1heCwgdG9rcykge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKHRva3MgJiYgdG9rcy5sZW5ndGggPiB0aGlzQi5maWVsZCAtIDMpXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHRva3NbdGhpc0IuZmllbGQgLSAzXSA9PSBuYW1lO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LCBcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgW3tvZmZzZXQ6IHN0YXJ0LCBzaXplOiBsZW5ndGh9XSwgXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNhbGxiYWNrKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIG9mZnNldCArPSB2YWxTaXplO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhbXSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSk7XG4gICAgICAgIH1cblxuICAgICAgICBicHRSZWFkTm9kZSh0aGlzQi5vZmZzZXQgKyByb290Tm9kZU9mZnNldCk7XG4gICAgfSk7XG59XG5cbmlmICh0eXBlb2YobW9kdWxlKSAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICBtb2R1bGUuZXhwb3J0cyA9IHtcbiAgICAgICAgbWFrZUJ3ZzogbWFrZUJ3ZyxcbiAgICAgICAgQklHX0JFRF9NQUdJQzogQklHX0JFRF9NQUdJQyxcbiAgICAgICAgQklHX1dJR19NQUdJQzogQklHX1dJR19NQUdJQ1xuICAgIH1cbn1cbiIsIi8qIC0qLSBtb2RlOiBqYXZhc2NyaXB0OyBjLWJhc2ljLW9mZnNldDogNDsgaW5kZW50LXRhYnMtbW9kZTogbmlsIC0qLSAqL1xuXG4vLyBcbi8vIERhbGxpYW5jZSBHZW5vbWUgRXhwbG9yZXJcbi8vIChjKSBUaG9tYXMgRG93biAyMDA2LTIwMTFcbi8vXG4vLyBiaW4uanMgZ2VuZXJhbCBiaW5hcnkgZGF0YSBzdXBwb3J0XG4vL1xuXG5cInVzZSBzdHJpY3RcIjtcblxuaWYgKHR5cGVvZihyZXF1aXJlKSAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICB2YXIgdXRpbHMgPSByZXF1aXJlKCcuL3V0aWxzJyk7XG4gICAgdmFyIHNoYWxsb3dDb3B5ID0gdXRpbHMuc2hhbGxvd0NvcHk7XG5cbiAgICB2YXIgc2hhMSA9IHJlcXVpcmUoJy4vc2hhMScpO1xuICAgIHZhciBiNjRfc2hhMSA9IHNoYTEuYjY0X3NoYTE7XG59XG5cbmZ1bmN0aW9uIEJsb2JGZXRjaGFibGUoYikge1xuICAgIHRoaXMuYmxvYiA9IGI7XG59XG5cbkJsb2JGZXRjaGFibGUucHJvdG90eXBlLnNsaWNlID0gZnVuY3Rpb24oc3RhcnQsIGxlbmd0aCkge1xuICAgIHZhciBiO1xuXG4gICAgaWYgKHRoaXMuYmxvYi5zbGljZSkge1xuICAgICAgICBpZiAobGVuZ3RoKSB7XG4gICAgICAgICAgICBiID0gdGhpcy5ibG9iLnNsaWNlKHN0YXJ0LCBzdGFydCArIGxlbmd0aCk7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBiID0gdGhpcy5ibG9iLnNsaWNlKHN0YXJ0KTtcbiAgICAgICAgfVxuICAgIH0gZWxzZSB7XG4gICAgICAgIGlmIChsZW5ndGgpIHtcbiAgICAgICAgICAgIGIgPSB0aGlzLmJsb2Iud2Via2l0U2xpY2Uoc3RhcnQsIHN0YXJ0ICsgbGVuZ3RoKTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIGIgPSB0aGlzLmJsb2Iud2Via2l0U2xpY2Uoc3RhcnQpO1xuICAgICAgICB9XG4gICAgfVxuICAgIHJldHVybiBuZXcgQmxvYkZldGNoYWJsZShiKTtcbn1cblxuQmxvYkZldGNoYWJsZS5wcm90b3R5cGUuc2FsdGVkID0gZnVuY3Rpb24oKSB7cmV0dXJuIHRoaXM7fVxuXG5pZiAodHlwZW9mKEZpbGVSZWFkZXIpICE9PSAndW5kZWZpbmVkJykge1xuICAgIC8vIGNvbnNvbGUubG9nKCdkZWZpbmluZyBhc3luYyBCbG9iRmV0Y2hhYmxlLmZldGNoJyk7XG5cbiAgICBCbG9iRmV0Y2hhYmxlLnByb3RvdHlwZS5mZXRjaCA9IGZ1bmN0aW9uKGNhbGxiYWNrKSB7XG4gICAgICAgIHZhciByZWFkZXIgPSBuZXcgRmlsZVJlYWRlcigpO1xuICAgICAgICByZWFkZXIub25sb2FkZW5kID0gZnVuY3Rpb24oZXYpIHtcbiAgICAgICAgICAgIGNhbGxiYWNrKGJzdHJpbmdUb0J1ZmZlcihyZWFkZXIucmVzdWx0KSk7XG4gICAgICAgIH07XG4gICAgICAgIHJlYWRlci5yZWFkQXNCaW5hcnlTdHJpbmcodGhpcy5ibG9iKTtcbiAgICB9XG5cbn0gZWxzZSB7XG4gICAgLy8gaWYgKGNvbnNvbGUgJiYgY29uc29sZS5sb2cpXG4gICAgLy8gICAgY29uc29sZS5sb2coJ2RlZmluaW5nIHN5bmMgQmxvYkZldGNoYWJsZS5mZXRjaCcpO1xuXG4gICAgQmxvYkZldGNoYWJsZS5wcm90b3R5cGUuZmV0Y2ggPSBmdW5jdGlvbihjYWxsYmFjaykge1xuICAgICAgICB2YXIgcmVhZGVyID0gbmV3IEZpbGVSZWFkZXJTeW5jKCk7XG4gICAgICAgIHRyeSB7XG4gICAgICAgICAgICB2YXIgcmVzID0gcmVhZGVyLnJlYWRBc0FycmF5QnVmZmVyKHRoaXMuYmxvYik7XG4gICAgICAgICAgICBjYWxsYmFjayhyZXMpO1xuICAgICAgICB9IGNhdGNoIChlKSB7XG4gICAgICAgICAgICBjYWxsYmFjayhudWxsLCBlKTtcbiAgICAgICAgfVxuICAgIH1cbn1cblxuZnVuY3Rpb24gVVJMRmV0Y2hhYmxlKHVybCwgc3RhcnQsIGVuZCwgb3B0cykge1xuICAgIGlmICghb3B0cykge1xuICAgICAgICBpZiAodHlwZW9mIHN0YXJ0ID09PSAnb2JqZWN0Jykge1xuICAgICAgICAgICAgb3B0cyA9IHN0YXJ0O1xuICAgICAgICAgICAgc3RhcnQgPSB1bmRlZmluZWQ7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBvcHRzID0ge307XG4gICAgICAgIH1cbiAgICB9XG5cbiAgICB0aGlzLnVybCA9IHVybDtcbiAgICB0aGlzLnN0YXJ0ID0gc3RhcnQgfHwgMDtcbiAgICBpZiAoZW5kKSB7XG4gICAgICAgIHRoaXMuZW5kID0gZW5kO1xuICAgIH1cbiAgICB0aGlzLm9wdHMgPSBvcHRzO1xufVxuXG5VUkxGZXRjaGFibGUucHJvdG90eXBlLnNsaWNlID0gZnVuY3Rpb24ocywgbCkge1xuICAgIGlmIChzIDwgMCkge1xuICAgICAgICB0aHJvdyAnQmFkIHNsaWNlICcgKyBzO1xuICAgIH1cblxuICAgIHZhciBucyA9IHRoaXMuc3RhcnQsIG5lID0gdGhpcy5lbmQ7XG4gICAgaWYgKG5zICYmIHMpIHtcbiAgICAgICAgbnMgPSBucyArIHM7XG4gICAgfSBlbHNlIHtcbiAgICAgICAgbnMgPSBzIHx8IG5zO1xuICAgIH1cbiAgICBpZiAobCAmJiBucykge1xuICAgICAgICBuZSA9IG5zICsgbCAtIDE7XG4gICAgfSBlbHNlIHtcbiAgICAgICAgbmUgPSBuZSB8fCBsIC0gMTtcbiAgICB9XG4gICAgcmV0dXJuIG5ldyBVUkxGZXRjaGFibGUodGhpcy51cmwsIG5zLCBuZSwgdGhpcy5vcHRzKTtcbn1cblxudmFyIHNlZWQ9MDtcbnZhciBpc1NhZmFyaSA9IG5hdmlnYXRvci51c2VyQWdlbnQuaW5kZXhPZignU2FmYXJpJykgPj0gMCAmJiBuYXZpZ2F0b3IudXNlckFnZW50LmluZGV4T2YoJ0Nocm9tZScpIDwgMCA7XG5cblVSTEZldGNoYWJsZS5wcm90b3R5cGUuZmV0Y2hBc1RleHQgPSBmdW5jdGlvbihjYWxsYmFjaykge1xuICAgIHZhciByZXEgPSBuZXcgWE1MSHR0cFJlcXVlc3QoKTtcbiAgICB2YXIgbGVuZ3RoO1xuICAgIHZhciB1cmwgPSB0aGlzLnVybDtcbiAgICBpZiAoaXNTYWZhcmkgfHwgdGhpcy5vcHRzLnNhbHQpIHtcbiAgICAgICAgdXJsID0gc2FsdFVSTCh1cmwpO1xuICAgICAgICB1cmwgPSB1cmwgKyAnP3NhbHQ9JyArIGI2NF9zaGExKCcnICsgRGF0ZS5ub3coKSArICcsJyArICgrK3NlZWQpKTtcbiAgICB9XG4gICAgcmVxLm9wZW4oJ0dFVCcsIHVybCwgdHJ1ZSk7XG5cbiAgICBpZiAodGhpcy5lbmQpIHtcbiAgICAgICAgaWYgKHRoaXMuZW5kIC0gdGhpcy5zdGFydCA+IDEwMDAwMDAwMCkge1xuICAgICAgICAgICAgdGhyb3cgJ01vbnN0ZXIgZmV0Y2ghJztcbiAgICAgICAgfVxuICAgICAgICByZXEuc2V0UmVxdWVzdEhlYWRlcignUmFuZ2UnLCAnYnl0ZXM9JyArIHRoaXMuc3RhcnQgKyAnLScgKyB0aGlzLmVuZCk7XG4gICAgICAgIGxlbmd0aCA9IHRoaXMuZW5kIC0gdGhpcy5zdGFydCArIDE7XG4gICAgfVxuXG4gICAgcmVxLm9ucmVhZHlzdGF0ZWNoYW5nZSA9IGZ1bmN0aW9uKCkge1xuICAgICAgICBpZiAocmVxLnJlYWR5U3RhdGUgPT0gNCkge1xuICAgICAgICAgICAgaWYgKHJlcS5zdGF0dXMgPT0gMjAwIHx8IHJlcS5zdGF0dXMgPT0gMjA2KSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKHJlcS5yZXNwb25zZVRleHQpO1xuICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gY2FsbGJhY2sobnVsbCk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9O1xuICAgIGlmICh0aGlzLm9wdHMuY3JlZGVudGlhbHMpIHtcbiAgICAgICAgcmVxLndpdGhDcmVkZW50aWFscyA9IHRydWU7XG4gICAgfVxuICAgIHJlcS5zZW5kKCcnKTtcbn1cblxuVVJMRmV0Y2hhYmxlLnByb3RvdHlwZS5zYWx0ZWQgPSBmdW5jdGlvbigpIHtcbiAgICB2YXIgbyA9IHNoYWxsb3dDb3B5KHRoaXMub3B0cyk7XG4gICAgby5zYWx0ID0gdHJ1ZTtcbiAgICByZXR1cm4gbmV3IFVSTEZldGNoYWJsZSh0aGlzLnVybCwgdGhpcy5zdGFydCwgdGhpcy5lbmQsIG8pO1xufVxuXG5VUkxGZXRjaGFibGUucHJvdG90eXBlLmZldGNoID0gZnVuY3Rpb24oY2FsbGJhY2ssIGF0dGVtcHQsIHRydW5jYXRlZExlbmd0aCkge1xuICAgIHZhciB0aGlzQiA9IHRoaXM7XG5cbiAgICBhdHRlbXB0ID0gYXR0ZW1wdCB8fCAxO1xuICAgIGlmIChhdHRlbXB0ID4gMykge1xuICAgICAgICByZXR1cm4gY2FsbGJhY2sobnVsbCk7XG4gICAgfVxuXG4gICAgdmFyIHJlcSA9IG5ldyBYTUxIdHRwUmVxdWVzdCgpO1xuICAgIHZhciBsZW5ndGg7XG4gICAgdmFyIHVybCA9IHRoaXMudXJsO1xuICAgIGlmIChpc1NhZmFyaSB8fCB0aGlzLm9wdHMuc2FsdCkge1xuICAgICAgICB1cmwgPSB1cmwgKyAnP3NhbHQ9JyArIGI2NF9zaGExKCcnICsgRGF0ZS5ub3coKSArICcsJyArICgrK3NlZWQpKTtcbiAgICB9XG4gICAgcmVxLm9wZW4oJ0dFVCcsIHVybCwgdHJ1ZSk7XG4gICAgcmVxLm92ZXJyaWRlTWltZVR5cGUoJ3RleHQvcGxhaW47IGNoYXJzZXQ9eC11c2VyLWRlZmluZWQnKTtcbiAgICBpZiAodGhpcy5lbmQpIHtcbiAgICAgICAgaWYgKHRoaXMuZW5kIC0gdGhpcy5zdGFydCA+IDEwMDAwMDAwMCkge1xuICAgICAgICAgICAgdGhyb3cgJ01vbnN0ZXIgZmV0Y2ghJztcbiAgICAgICAgfVxuICAgICAgICByZXEuc2V0UmVxdWVzdEhlYWRlcignUmFuZ2UnLCAnYnl0ZXM9JyArIHRoaXMuc3RhcnQgKyAnLScgKyB0aGlzLmVuZCk7XG4gICAgICAgIGxlbmd0aCA9IHRoaXMuZW5kIC0gdGhpcy5zdGFydCArIDE7XG4gICAgfVxuICAgIHJlcS5yZXNwb25zZVR5cGUgPSAnYXJyYXlidWZmZXInO1xuICAgIHJlcS5vbnJlYWR5c3RhdGVjaGFuZ2UgPSBmdW5jdGlvbigpIHtcbiAgICAgICAgaWYgKHJlcS5yZWFkeVN0YXRlID09IDQpIHtcbiAgICAgICAgICAgIGlmIChyZXEuc3RhdHVzID09IDIwMCB8fCByZXEuc3RhdHVzID09IDIwNikge1xuICAgICAgICAgICAgICAgIGlmIChyZXEucmVzcG9uc2UpIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGJsID0gcmVxLnJlc3BvbnNlLmJ5dGVMZW5ndGg7XG4gICAgICAgICAgICAgICAgICAgIGlmIChsZW5ndGggJiYgbGVuZ3RoICE9IGJsICYmICghdHJ1bmNhdGVkTGVuZ3RoIHx8IGJsICE9IHRydW5jYXRlZExlbmd0aCkpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiB0aGlzQi5mZXRjaChjYWxsYmFjaywgYXR0ZW1wdCArIDEsIGJsKTtcbiAgICAgICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhyZXEucmVzcG9uc2UpO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfSBlbHNlIGlmIChyZXEubW96UmVzcG9uc2VBcnJheUJ1ZmZlcikge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gY2FsbGJhY2socmVxLm1velJlc3BvbnNlQXJyYXlCdWZmZXIpO1xuICAgICAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciByID0gcmVxLnJlc3BvbnNlVGV4dDtcbiAgICAgICAgICAgICAgICAgICAgaWYgKGxlbmd0aCAmJiBsZW5ndGggIT0gci5sZW5ndGggJiYgKCF0cnVuY2F0ZWRMZW5ndGggfHwgci5sZW5ndGggIT0gdHJ1bmNhdGVkTGVuZ3RoKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHRoaXNCLmZldGNoKGNhbGxiYWNrLCBhdHRlbXB0ICsgMSwgci5sZW5ndGgpO1xuICAgICAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKGJzdHJpbmdUb0J1ZmZlcihyZXEucmVzcG9uc2VUZXh0KSk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgIHJldHVybiB0aGlzQi5mZXRjaChjYWxsYmFjaywgYXR0ZW1wdCArIDEpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfTtcbiAgICBpZiAodGhpcy5vcHRzLmNyZWRlbnRpYWxzKSB7XG4gICAgICAgIHJlcS53aXRoQ3JlZGVudGlhbHMgPSB0cnVlO1xuICAgIH1cbiAgICByZXEuc2VuZCgnJyk7XG59XG5cbmZ1bmN0aW9uIGJzdHJpbmdUb0J1ZmZlcihyZXN1bHQpIHtcbiAgICBpZiAoIXJlc3VsdCkge1xuICAgICAgICByZXR1cm4gbnVsbDtcbiAgICB9XG5cbiAgICB2YXIgYmEgPSBuZXcgVWludDhBcnJheShyZXN1bHQubGVuZ3RoKTtcbiAgICBmb3IgKHZhciBpID0gMDsgaSA8IGJhLmxlbmd0aDsgKytpKSB7XG4gICAgICAgIGJhW2ldID0gcmVzdWx0LmNoYXJDb2RlQXQoaSk7XG4gICAgfVxuICAgIHJldHVybiBiYS5idWZmZXI7XG59XG5cbi8vIFJlYWQgZnJvbSBVaW50OEFycmF5XG5cbihmdW5jdGlvbihnbG9iYWwpIHtcbiAgICB2YXIgY29udmVydEJ1ZmZlciA9IG5ldyBBcnJheUJ1ZmZlcig4KTtcbiAgICB2YXIgYmEgPSBuZXcgVWludDhBcnJheShjb252ZXJ0QnVmZmVyKTtcbiAgICB2YXIgZmEgPSBuZXcgRmxvYXQzMkFycmF5KGNvbnZlcnRCdWZmZXIpO1xuXG5cbiAgICBnbG9iYWwucmVhZEZsb2F0ID0gZnVuY3Rpb24oYnVmLCBvZmZzZXQpIHtcbiAgICAgICAgYmFbMF0gPSBidWZbb2Zmc2V0XTtcbiAgICAgICAgYmFbMV0gPSBidWZbb2Zmc2V0KzFdO1xuICAgICAgICBiYVsyXSA9IGJ1ZltvZmZzZXQrMl07XG4gICAgICAgIGJhWzNdID0gYnVmW29mZnNldCszXTtcbiAgICAgICAgcmV0dXJuIGZhWzBdO1xuICAgIH07XG4gfSh0aGlzKSk7XG5cbmZ1bmN0aW9uIHJlYWRJbnQ2NChiYSwgb2Zmc2V0KSB7XG4gICAgcmV0dXJuIChiYVtvZmZzZXQgKyA3XSA8PCAyNCkgfCAoYmFbb2Zmc2V0ICsgNl0gPDwgMTYpIHwgKGJhW29mZnNldCArIDVdIDw8IDgpIHwgKGJhW29mZnNldCArIDRdKTtcbn1cblxuZnVuY3Rpb24gcmVhZEludChiYSwgb2Zmc2V0KSB7XG4gICAgcmV0dXJuIChiYVtvZmZzZXQgKyAzXSA8PCAyNCkgfCAoYmFbb2Zmc2V0ICsgMl0gPDwgMTYpIHwgKGJhW29mZnNldCArIDFdIDw8IDgpIHwgKGJhW29mZnNldF0pO1xufVxuXG5mdW5jdGlvbiByZWFkU2hvcnQoYmEsIG9mZnNldCkge1xuICAgIHJldHVybiAoYmFbb2Zmc2V0ICsgMV0gPDwgOCkgfCAoYmFbb2Zmc2V0XSk7XG59XG5cbmZ1bmN0aW9uIHJlYWRCeXRlKGJhLCBvZmZzZXQpIHtcbiAgICByZXR1cm4gYmFbb2Zmc2V0XTtcbn1cblxuZnVuY3Rpb24gcmVhZEludEJFKGJhLCBvZmZzZXQpIHtcbiAgICByZXR1cm4gKGJhW29mZnNldF0gPDwgMjQpIHwgKGJhW29mZnNldCArIDFdIDw8IDE2KSB8IChiYVtvZmZzZXQgKyAyXSA8PCA4KSB8IChiYVtvZmZzZXQgKyAzXSk7XG59XG5cbi8vIEV4cG9ydHMgaWYgd2UgYXJlIGJlaW5nIHVzZWQgYXMgYSBtb2R1bGVcblxuaWYgKHR5cGVvZihtb2R1bGUpICE9PSAndW5kZWZpbmVkJykge1xuICAgIG1vZHVsZS5leHBvcnRzID0ge1xuICAgICAgICBCbG9iRmV0Y2hhYmxlOiBCbG9iRmV0Y2hhYmxlLFxuICAgICAgICBVUkxGZXRjaGFibGU6IFVSTEZldGNoYWJsZSxcblxuICAgICAgICByZWFkSW50OiByZWFkSW50LFxuICAgICAgICByZWFkSW50QkU6IHJlYWRJbnRCRSxcbiAgICAgICAgcmVhZEludDY0OiByZWFkSW50NjQsXG4gICAgICAgIHJlYWRTaG9ydDogcmVhZFNob3J0LFxuICAgICAgICByZWFkQnl0ZTogcmVhZEJ5dGUsXG4gICAgICAgIHJlYWRGbG9hdDogdGhpcy5yZWFkRmxvYXRcbiAgICB9XG59XG4iLCIvKiAtKi0gbW9kZTogamF2YXNjcmlwdDsgYy1iYXNpYy1vZmZzZXQ6IDQ7IGluZGVudC10YWJzLW1vZGU6IG5pbCAtKi0gKi9cblxuLy8gXG4vLyBEYWxsaWFuY2UgR2Vub21lIEV4cGxvcmVyXG4vLyAoYykgVGhvbWFzIERvd24gMjAwNi0yMDEwXG4vL1xuLy8gY29sb3IuanNcbi8vXG5cblwidXNlIHN0cmljdFwiO1xuXG5mdW5jdGlvbiBEQ29sb3VyKHJlZCwgZ3JlZW4sIGJsdWUsIG5hbWUpIHtcbiAgICB0aGlzLnJlZCA9IHJlZHwwO1xuICAgIHRoaXMuZ3JlZW4gPSBncmVlbnwwO1xuICAgIHRoaXMuYmx1ZSA9IGJsdWV8MDtcbiAgICBpZiAobmFtZSkge1xuICAgICAgICB0aGlzLm5hbWUgPSBuYW1lO1xuICAgIH1cbn1cblxuRENvbG91ci5wcm90b3R5cGUudG9TdmdTdHJpbmcgPSBmdW5jdGlvbigpIHtcbiAgICBpZiAoIXRoaXMubmFtZSkge1xuICAgICAgICB0aGlzLm5hbWUgPSBcInJnYihcIiArIHRoaXMucmVkICsgXCIsXCIgKyB0aGlzLmdyZWVuICsgXCIsXCIgKyB0aGlzLmJsdWUgKyBcIilcIjtcbiAgICB9XG5cbiAgICByZXR1cm4gdGhpcy5uYW1lO1xufVxuXG5mdW5jdGlvbiBoZXgyKHgpIHtcbiAgICB2YXIgeSA9ICcwMCcgKyB4LnRvU3RyaW5nKDE2KTtcbiAgICByZXR1cm4geS5zdWJzdHJpbmcoeS5sZW5ndGggLSAyKTtcbn1cblxuRENvbG91ci5wcm90b3R5cGUudG9IZXhTdHJpbmcgPSBmdW5jdGlvbigpIHtcbiAgICByZXR1cm4gJyMnICsgaGV4Mih0aGlzLnJlZCkgKyBoZXgyKHRoaXMuZ3JlZW4pICsgaGV4Mih0aGlzLmJsdWUpO1xufVxuXG52YXIgcGFsZXR0ZSA9IHtcbiAgICByZWQ6IG5ldyBEQ29sb3VyKDI1NSwgMCwgMCwgJ3JlZCcpLFxuICAgIGdyZWVuOiBuZXcgRENvbG91cigwLCAyNTUsIDAsICdncmVlbicpLFxuICAgIGJsdWU6IG5ldyBEQ29sb3VyKDAsIDAsIDI1NSwgJ2JsdWUnKSxcbiAgICB5ZWxsb3c6IG5ldyBEQ29sb3VyKDI1NSwgMjU1LCAwLCAneWVsbG93JyksXG4gICAgd2hpdGU6IG5ldyBEQ29sb3VyKDI1NSwgMjU1LCAyNTUsICd3aGl0ZScpLFxuICAgIGJsYWNrOiBuZXcgRENvbG91cigwLCAwLCAwLCAnYmxhY2snKSxcbiAgICBncmF5OiBuZXcgRENvbG91cigxODAsIDE4MCwgMTgwLCAnZ3JheScpLFxuICAgIGdyZXk6IG5ldyBEQ29sb3VyKDE4MCwgMTgwLCAxODAsICdncmV5Jylcbn07XG5cbnZhciBDT0xPUl9SRSA9IG5ldyBSZWdFeHAoJ14jKFswLTlBLUZhLWZdezJ9KShbMC05QS1GYS1mXXsyfSkoWzAtOUEtRmEtZl17Mn0pJCcpO1xudmFyIENTU19DT0xPUl9SRSA9IC9yZ2JcXCgoWzAtOV0rKSwoWzAtOV0rKSwoWzAtOV0rKVxcKS9cblxuZnVuY3Rpb24gZGFzQ29sb3VyRm9yTmFtZShuYW1lKSB7XG4gICAgdmFyIGMgPSBwYWxldHRlW25hbWVdO1xuICAgIGlmICghYykge1xuICAgICAgICB2YXIgbWF0Y2ggPSBDT0xPUl9SRS5leGVjKG5hbWUpO1xuICAgICAgICBpZiAobWF0Y2gpIHtcbiAgICAgICAgICAgIGMgPSBuZXcgRENvbG91cigoJzB4JyArIG1hdGNoWzFdKXwwLCAoJzB4JyArIG1hdGNoWzJdKXwwLCAoJzB4JyArIG1hdGNoWzNdKXwwLCBuYW1lKTtcbiAgICAgICAgICAgIHBhbGV0dGVbbmFtZV0gPSBjO1xuICAgICAgICB9IGVsc2Uge1xuICAgIFx0ICAgIG1hdGNoID0gQ1NTX0NPTE9SX1JFLmV4ZWMobmFtZSk7XG4gICAgXHQgICAgaWYgKG1hdGNoKSB7XG4gICAgICAgIFx0XHRjID0gbmV3IERDb2xvdXIobWF0Y2hbMV18MCwgbWF0Y2hbMl18MCwgbWF0Y2hbM118MCwgbmFtZSk7XG4gICAgICAgIFx0XHRwYWxldHRlW25hbWVdID0gYztcblx0ICAgICAgIH0gZWxzZSB7XG5cdFx0ICAgICAgY29uc29sZS5sb2coXCJjb3VsZG4ndCBoYW5kbGUgY29sb3I6IFwiICsgbmFtZSk7XG5cdFx0ICAgICAgYyA9IHBhbGV0dGUuYmxhY2s7XG5cdFx0ICAgICAgcGFsZXR0ZVtuYW1lXSA9IGM7XG5cdCAgICAgICB9XG4gICAgICAgIH1cbiAgICB9XG4gICAgcmV0dXJuIGM7XG59XG5cbmZ1bmN0aW9uIG1ha2VDb2xvdXJTdGVwcyhzdGVwcywgc3RvcHMsIGNvbG91cnMpIHtcbiAgICB2YXIgZGNvbG91cnMgPSBbXTtcbiAgICBmb3IgKHZhciBjaSA9IDA7IGNpIDwgY29sb3Vycy5sZW5ndGg7ICsrY2kpIHtcbiAgICAgICAgZGNvbG91cnMucHVzaChkYXNDb2xvdXJGb3JOYW1lKGNvbG91cnNbY2ldKSk7XG4gICAgfVxuXG4gICAgdmFyIGdyYWQgPSBbXTtcbiAgU1RFUF9MT09QOlxuICAgIGZvciAodmFyIHNpID0gMDsgc2kgPCBzdGVwczsgKytzaSkge1xuICAgICAgICB2YXIgcnMgPSAoMS4wICogc2kpIC8gKHN0ZXBzLTEpO1xuICAgICAgICB2YXIgc2NvcmUgPSBzdG9wc1swXSArIChzdG9wc1tzdG9wcy5sZW5ndGggLTFdIC0gc3RvcHNbMF0pICogcnM7XG4gICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgc3RvcHMubGVuZ3RoIC0gMTsgKytpKSB7XG4gICAgICAgICAgICBpZiAoc2NvcmUgPj0gc3RvcHNbaV0gJiYgc2NvcmUgPD0gc3RvcHNbaSsxXSkge1xuICAgICAgICAgICAgICAgIHZhciBmcmFjID0gKHNjb3JlIC0gc3RvcHNbaV0pIC8gKHN0b3BzW2krMV0gLSBzdG9wc1tpXSk7XG4gICAgICAgICAgICAgICAgdmFyIGNhID0gZGNvbG91cnNbaV07XG4gICAgICAgICAgICAgICAgdmFyIGNiID0gZGNvbG91cnNbaSsxXTtcblxuICAgICAgICAgICAgICAgIHZhciBmaWxsID0gbmV3IERDb2xvdXIoXG4gICAgICAgICAgICAgICAgICAgICgoY2EucmVkICogKDEuMCAtIGZyYWMpKSArIChjYi5yZWQgKiBmcmFjKSl8MCxcbiAgICAgICAgICAgICAgICAgICAgKChjYS5ncmVlbiAqICgxLjAgLSBmcmFjKSkgKyAoY2IuZ3JlZW4gKiBmcmFjKSl8MCxcbiAgICAgICAgICAgICAgICAgICAgKChjYS5ibHVlICogKDEuMCAtIGZyYWMpKSArIChjYi5ibHVlICogZnJhYykpfDBcbiAgICAgICAgICAgICAgICApLnRvU3ZnU3RyaW5nKCk7XG4gICAgICAgICAgICAgICAgZ3JhZC5wdXNoKGZpbGwpO1xuXG4gICAgICAgICAgICAgICAgY29udGludWUgU1RFUF9MT09QO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIHRocm93ICdCYWQgc3RlcCc7XG4gICAgfVxuXG4gICAgcmV0dXJuIGdyYWQ7XG59XG5cbmZ1bmN0aW9uIG1ha2VHcmFkaWVudChzdGVwcywgY29sb3IxLCBjb2xvcjIsIGNvbG9yMykge1xuICAgIGlmIChjb2xvcjMpIHtcbiAgICAgICAgcmV0dXJuIG1ha2VDb2xvdXJTdGVwcyhzdGVwcywgWzAsIDAuNSwgMV0sIFtjb2xvcjEsIGNvbG9yMiwgY29sb3IzXSk7XG4gICAgfSBlbHNlIHtcbiAgICAgICAgcmV0dXJuIG1ha2VDb2xvdXJTdGVwcyhzdGVwcywgWzAsIDFdLCBbY29sb3IxLCBjb2xvcjJdKTtcbiAgICB9XG59XG5cbmlmICh0eXBlb2YobW9kdWxlKSAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICBtb2R1bGUuZXhwb3J0cyA9IHtcbiAgICAgICAgbWFrZUNvbG91clN0ZXBzOiBtYWtlQ29sb3VyU3RlcHMsXG4gICAgICAgIG1ha2VHcmFkaWVudDogbWFrZUdyYWRpZW50LFxuICAgICAgICBkYXNDb2xvdXJGb3JOYW1lOiBkYXNDb2xvdXJGb3JOYW1lXG4gICAgfTtcbn1cbiIsIi8qIC0qLSBtb2RlOiBqYXZhc2NyaXB0OyBjLWJhc2ljLW9mZnNldDogNDsgaW5kZW50LXRhYnMtbW9kZTogbmlsIC0qLSAqL1xuXG4vLyBcbi8vIERhbGxpYW5jZSBHZW5vbWUgRXhwbG9yZXJcbi8vIChjKSBUaG9tYXMgRG93biAyMDA2LTIwMTBcbi8vXG4vLyBkYXMuanM6IHF1ZXJpZXMgYW5kIGxvdy1sZXZlbCBkYXRhIG1vZGVsLlxuLy9cblxuXCJ1c2Ugc3RyaWN0XCI7XG5cbmlmICh0eXBlb2YocmVxdWlyZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgdmFyIHV0aWxzID0gcmVxdWlyZSgnLi91dGlscycpO1xuICAgIHZhciBzaGFsbG93Q29weSA9IHV0aWxzLnNoYWxsb3dDb3B5O1xuICAgIHZhciBwdXNobyA9IHV0aWxzLnB1c2hvO1xuXG4gICAgdmFyIGNvbG9yID0gcmVxdWlyZSgnLi9jb2xvcicpO1xuICAgIHZhciBtYWtlQ29sb3VyU3RlcHMgPSBjb2xvci5tYWtlQ29sb3VyU3RlcHM7XG59XG5cbnZhciBkYXNMaWJFcnJvckhhbmRsZXIgPSBmdW5jdGlvbihlcnJNc2cpIHtcbiAgICBhbGVydChlcnJNc2cpO1xufVxudmFyIGRhc0xpYlJlcXVlc3RRdWV1ZSA9IG5ldyBBcnJheSgpO1xuXG5cblxuZnVuY3Rpb24gREFTU2VnbWVudChuYW1lLCBzdGFydCwgZW5kLCBkZXNjcmlwdGlvbikge1xuICAgIHRoaXMubmFtZSA9IG5hbWU7XG4gICAgdGhpcy5zdGFydCA9IHN0YXJ0O1xuICAgIHRoaXMuZW5kID0gZW5kO1xuICAgIHRoaXMuZGVzY3JpcHRpb24gPSBkZXNjcmlwdGlvbjtcbn1cbkRBU1NlZ21lbnQucHJvdG90eXBlLnRvU3RyaW5nID0gZnVuY3Rpb24oKSB7XG4gICAgcmV0dXJuIHRoaXMubmFtZSArICc6JyArIHRoaXMuc3RhcnQgKyAnLi4nICsgdGhpcy5lbmQ7XG59O1xuREFTU2VnbWVudC5wcm90b3R5cGUuaXNCb3VuZGVkID0gZnVuY3Rpb24oKSB7XG4gICAgcmV0dXJuIHRoaXMuc3RhcnQgJiYgdGhpcy5lbmQ7XG59XG5EQVNTZWdtZW50LnByb3RvdHlwZS50b0RBU1F1ZXJ5ID0gZnVuY3Rpb24oKSB7XG4gICAgdmFyIHEgPSAnc2VnbWVudD0nICsgdGhpcy5uYW1lO1xuICAgIGlmICh0aGlzLnN0YXJ0ICYmIHRoaXMuZW5kKSB7XG4gICAgICAgIHEgKz0gKCc6JyArIHRoaXMuc3RhcnQgKyAnLCcgKyB0aGlzLmVuZCk7XG4gICAgfVxuICAgIHJldHVybiBxO1xufVxuXG5cbmZ1bmN0aW9uIERBU1NvdXJjZShhMSwgYTIpIHtcbiAgICB2YXIgb3B0aW9ucztcbiAgICBpZiAodHlwZW9mIGExID09ICdzdHJpbmcnKSB7XG4gICAgICAgIHRoaXMudXJpID0gYTE7XG4gICAgICAgIG9wdGlvbnMgPSBhMiB8fCB7fTtcbiAgICB9IGVsc2Uge1xuICAgICAgICBvcHRpb25zID0gYTEgfHwge307XG4gICAgfVxuICAgIGZvciAodmFyIGsgaW4gb3B0aW9ucykge1xuICAgICAgICBpZiAodHlwZW9mKG9wdGlvbnNba10pICE9ICdmdW5jdGlvbicpIHtcbiAgICAgICAgICAgIHRoaXNba10gPSBvcHRpb25zW2tdO1xuICAgICAgICB9XG4gICAgfVxuXG5cbiAgICBpZiAoIXRoaXMuY29vcmRzKSB7XG4gICAgICAgIHRoaXMuY29vcmRzID0gW107XG4gICAgfVxuICAgIGlmICghdGhpcy5wcm9wcykge1xuICAgICAgICB0aGlzLnByb3BzID0ge307XG4gICAgfVxuXG4gICAgdGhpcy5kYXNCYXNlVVJJID0gdGhpcy51cmk7XG4gICAgaWYgKHRoaXMuZGFzQmFzZVVSSSAmJiB0aGlzLmRhc0Jhc2VVUkkuc3Vic3RyKHRoaXMudXJpLmxlbmd0aCAtIDEpICE9ICcvJykge1xuICAgICAgICB0aGlzLmRhc0Jhc2VVUkkgPSB0aGlzLmRhc0Jhc2VVUkkgKyAnLyc7XG4gICAgfVxufVxuXG5mdW5jdGlvbiBEQVNDb29yZHMoKSB7XG59XG5cbmZ1bmN0aW9uIGNvb3Jkc01hdGNoKGMxLCBjMikge1xuICAgIHJldHVybiBjMS50YXhvbiA9PSBjMi50YXhvbiAmJiBjMS5hdXRoID09IGMyLmF1dGggJiYgYzEudmVyc2lvbiA9PSBjMi52ZXJzaW9uO1xufVxuXG4vL1xuLy8gREFTIDEuNiBlbnRyeV9wb2ludHMgY29tbWFuZFxuLy9cblxuREFTU291cmNlLnByb3RvdHlwZS5lbnRyeVBvaW50cyA9IGZ1bmN0aW9uKGNhbGxiYWNrKSB7XG4gICAgdmFyIGRhc1VSSSA9IHRoaXMuZGFzQmFzZVVSSSArICdlbnRyeV9wb2ludHMnO1xuICAgIHRoaXMuZG9Dcm9zc0RvbWFpblJlcXVlc3QoZGFzVVJJLCBmdW5jdGlvbihyZXNwb25zZVhNTCkge1xuICAgICAgICAgICAgaWYgKCFyZXNwb25zZVhNTCkge1xuICAgICAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhbXSk7XG4gICAgICAgICAgICB9XG5cbiAgICAgICAgICAgICAgICB2YXIgZW50cnlQb2ludHMgPSBuZXcgQXJyYXkoKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICB2YXIgc2VncyA9IHJlc3BvbnNlWE1MLmdldEVsZW1lbnRzQnlUYWdOYW1lKCdTRUdNRU5UJyk7XG4gICAgICAgICAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBzZWdzLmxlbmd0aDsgKytpKSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciBzZWcgPSBzZWdzW2ldO1xuICAgICAgICAgICAgICAgICAgICB2YXIgc2VnSWQgPSBzZWcuZ2V0QXR0cmlidXRlKCdpZCcpO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgdmFyIHNlZ1NpemUgPSBzZWcuZ2V0QXR0cmlidXRlKCdzaXplJyk7XG4gICAgICAgICAgICAgICAgICAgIHZhciBzZWdNaW4sIHNlZ01heDtcbiAgICAgICAgICAgICAgICAgICAgaWYgKHNlZ1NpemUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHNlZ01pbiA9IDE7IHNlZ01heCA9IHNlZ1NpemV8MDtcbiAgICAgICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHNlZ01pbiA9IHNlZy5nZXRBdHRyaWJ1dGUoJ3N0YXJ0Jyk7XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoc2VnTWluKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgc2VnTWluIHw9IDA7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBzZWdNYXggPSBzZWcuZ2V0QXR0cmlidXRlKCdzdG9wJyk7XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoc2VnTWF4KSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgc2VnTWF4IHw9IDA7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgdmFyIHNlZ0Rlc2MgPSBudWxsO1xuICAgICAgICAgICAgICAgICAgICBpZiAoc2VnLmZpcnN0Q2hpbGQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHNlZ0Rlc2MgPSBzZWcuZmlyc3RDaGlsZC5ub2RlVmFsdWU7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZW50cnlQb2ludHMucHVzaChuZXcgREFTU2VnbWVudChzZWdJZCwgc2VnTWluLCBzZWdNYXgsIHNlZ0Rlc2MpKTtcbiAgICAgICAgICAgICAgICB9ICAgICAgICAgIFxuICAgICAgICAgICAgICAgY2FsbGJhY2soZW50cnlQb2ludHMpO1xuICAgIH0pOyAgICAgICAgIFxufVxuXG4vL1xuLy8gREFTIDEuNiBzZXF1ZW5jZSBjb21tYW5kXG4vLyBEbyB3ZSBuZWVkIGFuIG9wdGlvbiB0byBmYWxsIGJhY2sgdG8gdGhlIGRuYSBjb21tYW5kP1xuLy9cblxuZnVuY3Rpb24gREFTU2VxdWVuY2UobmFtZSwgc3RhcnQsIGVuZCwgYWxwaGEsIHNlcSkge1xuICAgIHRoaXMubmFtZSA9IG5hbWU7XG4gICAgdGhpcy5zdGFydCA9IHN0YXJ0O1xuICAgIHRoaXMuZW5kID0gZW5kO1xuICAgIHRoaXMuYWxwaGFiZXQgPSBhbHBoYTtcbiAgICB0aGlzLnNlcSA9IHNlcTtcbn1cblxuREFTU291cmNlLnByb3RvdHlwZS5zZXF1ZW5jZSA9IGZ1bmN0aW9uKHNlZ21lbnQsIGNhbGxiYWNrKSB7XG4gICAgdmFyIGRhc1VSSSA9IHRoaXMuZGFzQmFzZVVSSSArICdzZXF1ZW5jZT8nICsgc2VnbWVudC50b0RBU1F1ZXJ5KCk7XG4gICAgdGhpcy5kb0Nyb3NzRG9tYWluUmVxdWVzdChkYXNVUkksIGZ1bmN0aW9uKHJlc3BvbnNlWE1MKSB7XG4gICAgICAgIGlmICghcmVzcG9uc2VYTUwpIHtcbiAgICAgICAgICAgIGNhbGxiYWNrKFtdKTtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICB2YXIgc2VxcyA9IG5ldyBBcnJheSgpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIHZhciBzZWdzID0gcmVzcG9uc2VYTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ1NFUVVFTkNFJyk7XG4gICAgICAgICAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBzZWdzLmxlbmd0aDsgKytpKSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciBzZWcgPSBzZWdzW2ldO1xuICAgICAgICAgICAgICAgICAgICB2YXIgc2VnSWQgPSBzZWcuZ2V0QXR0cmlidXRlKCdpZCcpO1xuICAgICAgICAgICAgICAgICAgICB2YXIgc2VnTWluID0gc2VnLmdldEF0dHJpYnV0ZSgnc3RhcnQnKTtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHNlZ01heCA9IHNlZy5nZXRBdHRyaWJ1dGUoJ3N0b3AnKTtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHNlZ0FscGhhID0gJ0ROQSc7XG4gICAgICAgICAgICAgICAgICAgIHZhciBzZWdTZXEgPSBudWxsO1xuICAgICAgICAgICAgICAgICAgICBpZiAoc2VnLmZpcnN0Q2hpbGQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciByYXdTZXEgPSBzZWcuZmlyc3RDaGlsZC5ub2RlVmFsdWU7XG4gICAgICAgICAgICAgICAgICAgICAgICBzZWdTZXEgPSAnJztcbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciBpZHggPSAwO1xuICAgICAgICAgICAgICAgICAgICAgICAgd2hpbGUgKHRydWUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YXIgc3BhY2UgPSByYXdTZXEuaW5kZXhPZignXFxuJywgaWR4KTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAoc3BhY2UgPj0gMCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBzZWdTZXEgKz0gcmF3U2VxLnN1YnN0cmluZyhpZHgsIHNwYWNlKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaWR4ID0gc3BhY2UgKyAxO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHNlZ1NlcSArPSByYXdTZXEuc3Vic3RyaW5nKGlkeCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBzZXFzLnB1c2gobmV3IERBU1NlcXVlbmNlKHNlZ0lkLCBzZWdNaW4sIHNlZ01heCwgc2VnQWxwaGEsIHNlZ1NlcSkpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBjYWxsYmFjayhzZXFzKTtcbiAgICAgICAgfVxuICAgIH0pO1xufVxuXG4vL1xuLy8gREFTIDEuNiBmZWF0dXJlcyBjb21tYW5kXG4vL1xuXG5mdW5jdGlvbiBEQVNGZWF0dXJlKCkge1xufVxuXG5mdW5jdGlvbiBEQVNHcm91cChpZCkge1xuICAgIGlmIChpZClcbiAgICAgICAgdGhpcy5pZCA9IGlkO1xufVxuXG5mdW5jdGlvbiBEQVNMaW5rKGRlc2MsIHVyaSkge1xuICAgIHRoaXMuZGVzYyA9IGRlc2M7XG4gICAgdGhpcy51cmkgPSB1cmk7XG59XG5cbkRBU1NvdXJjZS5wcm90b3R5cGUuZmVhdHVyZXMgPSBmdW5jdGlvbihzZWdtZW50LCBvcHRpb25zLCBjYWxsYmFjaykge1xuICAgIG9wdGlvbnMgPSBvcHRpb25zIHx8IHt9O1xuICAgIHZhciB0aGlzQiA9IHRoaXM7XG5cbiAgICB2YXIgZGFzVVJJO1xuICAgIGlmICh0aGlzLmZlYXR1cmVzX3VyaSkge1xuICAgICAgICBkYXNVUkkgPSB0aGlzLmZlYXR1cmVzX3VyaTtcbiAgICB9IGVsc2Uge1xuICAgICAgICB2YXIgZmlsdGVycyA9IFtdO1xuXG4gICAgICAgIGlmIChzZWdtZW50KSB7XG4gICAgICAgICAgICBmaWx0ZXJzLnB1c2goc2VnbWVudC50b0RBU1F1ZXJ5KCkpO1xuICAgICAgICB9IGVsc2UgaWYgKG9wdGlvbnMuZ3JvdXApIHtcbiAgICAgICAgICAgIHZhciBnID0gb3B0aW9ucy5ncm91cDtcbiAgICAgICAgICAgIGlmICh0eXBlb2YgZyA9PSAnc3RyaW5nJykge1xuICAgICAgICAgICAgICAgIGZpbHRlcnMucHVzaCgnZ3JvdXBfaWQ9JyArIGcpO1xuICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICBmb3IgKHZhciBnaSA9IDA7IGdpIDwgZy5sZW5ndGg7ICsrZ2kpIHtcbiAgICAgICAgICAgICAgICAgICAgZmlsdGVycy5wdXNoKCdncm91cF9pZD0nICsgZ1tnaV0pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuXG4gICAgICAgIGlmIChvcHRpb25zLmFkamFjZW50KSB7XG4gICAgICAgICAgICB2YXIgYWRqID0gb3B0aW9ucy5hZGphY2VudDtcbiAgICAgICAgICAgIGlmICh0eXBlb2YgYWRqID09ICdzdHJpbmcnKSB7XG4gICAgICAgICAgICAgICAgYWRqID0gW2Fkal07XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBmb3IgKHZhciBhaSA9IDA7IGFpIDwgYWRqLmxlbmd0aDsgKythaSkge1xuICAgICAgICAgICAgICAgIGZpbHRlcnMucHVzaCgnYWRqYWNlbnQ9JyArIGFkalthaV0pO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG5cbiAgICAgICAgaWYgKG9wdGlvbnMudHlwZSkge1xuICAgICAgICAgICAgaWYgKHR5cGVvZiBvcHRpb25zLnR5cGUgPT0gJ3N0cmluZycpIHtcbiAgICAgICAgICAgICAgICBmaWx0ZXJzLnB1c2goJ3R5cGU9JyArIG9wdGlvbnMudHlwZSk7XG4gICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgIGZvciAodmFyIHRpID0gMDsgdGkgPCBvcHRpb25zLnR5cGUubGVuZ3RoOyArK3RpKSB7XG4gICAgICAgICAgICAgICAgICAgIGZpbHRlcnMucHVzaCgndHlwZT0nICsgb3B0aW9ucy50eXBlW3RpXSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICBpZiAob3B0aW9ucy5tYXhiaW5zKSB7XG4gICAgICAgICAgICBmaWx0ZXJzLnB1c2goJ21heGJpbnM9JyArIG9wdGlvbnMubWF4Ymlucyk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIGlmIChmaWx0ZXJzLmxlbmd0aCA+IDApIHtcbiAgICAgICAgICAgIGRhc1VSSSA9IHRoaXMuZGFzQmFzZVVSSSArICdmZWF0dXJlcz8nICsgZmlsdGVycy5qb2luKCc7Jyk7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBjYWxsYmFjayhbXSwgJ05vIGZpbHRlcnMgc3BlY2lmaWVkJyk7XG4gICAgICAgIH1cbiAgICB9IFxuICAgXG5cbiAgICB0aGlzLmRvQ3Jvc3NEb21haW5SZXF1ZXN0KGRhc1VSSSwgZnVuY3Rpb24ocmVzcG9uc2VYTUwsIHJlcSkge1xuICAgICAgICBpZiAoIXJlc3BvbnNlWE1MKSB7XG4gICAgICAgICAgICB2YXIgbXNnO1xuICAgICAgICAgICAgaWYgKHJlcS5zdGF0dXMgPT0gMCkge1xuICAgICAgICAgICAgICAgIG1zZyA9ICdzZXJ2ZXIgbWF5IG5vdCBzdXBwb3J0IENPUlMnO1xuICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICBtc2cgPSAnc3RhdHVzPScgKyByZXEuc3RhdHVzO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgY2FsbGJhY2soW10sICdGYWlsZWQgcmVxdWVzdDogJyArIG1zZyk7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbi8qICAgICAgaWYgKHJlcSkge1xuICAgICAgICAgICAgdmFyIGNhcHMgPSByZXEuZ2V0UmVzcG9uc2VIZWFkZXIoJ1gtREFTLUNhcGFiaWx0aWVzJyk7XG4gICAgICAgICAgICBpZiAoY2Fwcykge1xuICAgICAgICAgICAgICAgIGFsZXJ0KGNhcHMpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9ICovXG5cbiAgICAgICAgdmFyIGZlYXR1cmVzID0gbmV3IEFycmF5KCk7XG4gICAgICAgIHZhciBzZWdtZW50TWFwID0ge307XG5cbiAgICAgICAgdmFyIHNlZ3MgPSByZXNwb25zZVhNTC5nZXRFbGVtZW50c0J5VGFnTmFtZSgnU0VHTUVOVCcpO1xuICAgICAgICBmb3IgKHZhciBzaSA9IDA7IHNpIDwgc2Vncy5sZW5ndGg7ICsrc2kpIHtcbiAgICAgICAgICAgIHZhciBzZWdtZW50WE1MID0gc2Vnc1tzaV07XG4gICAgICAgICAgICB2YXIgc2VnbWVudElEID0gc2VnbWVudFhNTC5nZXRBdHRyaWJ1dGUoJ2lkJyk7XG4gICAgICAgICAgICBzZWdtZW50TWFwW3NlZ21lbnRJRF0gPSB7XG4gICAgICAgICAgICAgICAgbWluOiBzZWdtZW50WE1MLmdldEF0dHJpYnV0ZSgnc3RhcnQnKSxcbiAgICAgICAgICAgICAgICBtYXg6IHNlZ21lbnRYTUwuZ2V0QXR0cmlidXRlKCdzdG9wJylcbiAgICAgICAgICAgIH07XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBmZWF0dXJlWE1McyA9IHNlZ21lbnRYTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ0ZFQVRVUkUnKTtcbiAgICAgICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgZmVhdHVyZVhNTHMubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgICAgICAgICB2YXIgZmVhdHVyZSA9IGZlYXR1cmVYTUxzW2ldO1xuICAgICAgICAgICAgICAgIHZhciBkYXNGZWF0dXJlID0gbmV3IERBU0ZlYXR1cmUoKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLnNlZ21lbnQgPSBzZWdtZW50SUQ7XG4gICAgICAgICAgICAgICAgZGFzRmVhdHVyZS5pZCA9IGZlYXR1cmUuZ2V0QXR0cmlidXRlKCdpZCcpO1xuICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmUubGFiZWwgPSBmZWF0dXJlLmdldEF0dHJpYnV0ZSgnbGFiZWwnKTtcblxuXG4vKlxuICAgICAgICAgICAgICAgIHZhciBjaGlsZE5vZGVzID0gZmVhdHVyZS5jaGlsZE5vZGVzO1xuICAgICAgICAgICAgICAgIGZvciAodmFyIGMgPSAwOyBjIDwgY2hpbGROb2Rlcy5sZW5ndGg7ICsrYykge1xuICAgICAgICAgICAgICAgICAgICB2YXIgY24gPSBjaGlsZE5vZGVzW2NdO1xuICAgICAgICAgICAgICAgICAgICBpZiAoY24ubm9kZVR5cGUgPT0gTm9kZS5FTEVNRU5UX05PREUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciBrZXkgPSBjbi50YWdOYW1lO1xuICAgICAgICAgICAgICAgICAgICAgICAgLy92YXIgdmFsID0gbnVsbDtcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vaWYgKGNuLmZpcnN0Q2hpbGQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vICAgdmFsID0gY24uZmlyc3RDaGlsZC5ub2RlVmFsdWU7XG4gICAgICAgICAgICAgICAgICAgICAgICAvL31cbiAgICAgICAgICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmVba2V5XSA9ICd4JztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH0gKi9cblxuXG4gICAgICAgICAgICAgICAgdmFyIHNwb3MgPSBlbGVtZW50VmFsdWUoZmVhdHVyZSwgXCJTVEFSVFwiKTtcbiAgICAgICAgICAgICAgICB2YXIgZXBvcyA9IGVsZW1lbnRWYWx1ZShmZWF0dXJlLCBcIkVORFwiKTtcbiAgICAgICAgICAgICAgICBpZiAoKHNwb3N8MCkgPiAoZXBvc3wwKSkge1xuICAgICAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLm1pbiA9IGVwb3N8MDtcbiAgICAgICAgICAgICAgICAgICAgZGFzRmVhdHVyZS5tYXggPSBzcG9zfDA7XG4gICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgZGFzRmVhdHVyZS5taW4gPSBzcG9zfDA7XG4gICAgICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmUubWF4ID0gZXBvc3wwO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgIHZhciB0ZWMgPSBmZWF0dXJlLmdldEVsZW1lbnRzQnlUYWdOYW1lKCdUWVBFJyk7XG4gICAgICAgICAgICAgICAgICAgIGlmICh0ZWMubGVuZ3RoID4gMCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIHRlID0gdGVjWzBdO1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKHRlLmZpcnN0Q2hpbGQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLnR5cGUgPSB0ZS5maXJzdENoaWxkLm5vZGVWYWx1ZTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmUudHlwZUlkID0gdGUuZ2V0QXR0cmlidXRlKCdpZCcpO1xuICAgICAgICAgICAgICAgICAgICAgICAgZGFzRmVhdHVyZS50eXBlQ3YgPSB0ZS5nZXRBdHRyaWJ1dGUoJ2N2SWQnKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLnR5cGUgPSBlbGVtZW50VmFsdWUoZmVhdHVyZSwgXCJUWVBFXCIpO1xuICAgICAgICAgICAgICAgIGlmICghZGFzRmVhdHVyZS50eXBlICYmIGRhc0ZlYXR1cmUudHlwZUlkKSB7XG4gICAgICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmUudHlwZSA9IGRhc0ZlYXR1cmUudHlwZUlkOyAvLyBGSVhNRT9cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgZGFzRmVhdHVyZS5tZXRob2QgPSBlbGVtZW50VmFsdWUoZmVhdHVyZSwgXCJNRVRIT0RcIik7XG4gICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICB2YXIgb3JpID0gZWxlbWVudFZhbHVlKGZlYXR1cmUsIFwiT1JJRU5UQVRJT05cIik7XG4gICAgICAgICAgICAgICAgICAgIGlmICghb3JpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBvcmkgPSAnMCc7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZGFzRmVhdHVyZS5vcmllbnRhdGlvbiA9IG9yaTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZGFzRmVhdHVyZS5zY29yZSA9IGVsZW1lbnRWYWx1ZShmZWF0dXJlLCBcIlNDT1JFXCIpO1xuICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmUubGlua3MgPSBkYXNMaW5rc09mKGZlYXR1cmUpO1xuICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmUubm90ZXMgPSBkYXNOb3Rlc09mKGZlYXR1cmUpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIHZhciBncm91cHMgPSBmZWF0dXJlLmdldEVsZW1lbnRzQnlUYWdOYW1lKFwiR1JPVVBcIik7XG4gICAgICAgICAgICAgICAgZm9yICh2YXIgZ2kgID0gMDsgZ2kgPCBncm91cHMubGVuZ3RoOyArK2dpKSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciBncm91cFhNTCA9IGdyb3Vwc1tnaV07XG4gICAgICAgICAgICAgICAgICAgIHZhciBkYXNHcm91cCA9IG5ldyBEQVNHcm91cCgpO1xuICAgICAgICAgICAgICAgICAgICBkYXNHcm91cC50eXBlID0gZ3JvdXBYTUwuZ2V0QXR0cmlidXRlKCd0eXBlJyk7XG4gICAgICAgICAgICAgICAgICAgIGRhc0dyb3VwLmlkID0gZ3JvdXBYTUwuZ2V0QXR0cmlidXRlKCdpZCcpO1xuICAgICAgICAgICAgICAgICAgICBkYXNHcm91cC5saW5rcyA9IGRhc0xpbmtzT2YoZ3JvdXBYTUwpO1xuICAgICAgICAgICAgICAgICAgICBkYXNHcm91cC5ub3RlcyA9IGRhc05vdGVzT2YoZ3JvdXBYTUwpO1xuICAgICAgICAgICAgICAgICAgICBpZiAoIWRhc0ZlYXR1cmUuZ3JvdXBzKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLmdyb3VwcyA9IG5ldyBBcnJheShkYXNHcm91cCk7XG4gICAgICAgICAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLmdyb3Vwcy5wdXNoKGRhc0dyb3VwKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgICAgIC8vIE1hZ2ljIG5vdGVzLiAgQ2hlY2sgd2l0aCBUQUQgYmVmb3JlIGNoYW5naW5nIHRoaXMuXG4gICAgICAgICAgICAgICAgaWYgKGRhc0ZlYXR1cmUubm90ZXMpIHtcbiAgICAgICAgICAgICAgICAgICAgZm9yICh2YXIgbmkgPSAwOyBuaSA8IGRhc0ZlYXR1cmUubm90ZXMubGVuZ3RoOyArK25pKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgbiA9IGRhc0ZlYXR1cmUubm90ZXNbbmldO1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKG4uaW5kZXhPZignR2VuZW5hbWU9JykgPT0gMCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciBnZyA9IG5ldyBEQVNHcm91cCgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGdnLnR5cGU9J2dlbmUnO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGdnLmlkID0gbi5zdWJzdHJpbmcoOSk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCFkYXNGZWF0dXJlLmdyb3Vwcykge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLmdyb3VwcyA9IG5ldyBBcnJheShnZyk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgZGFzRmVhdHVyZS5ncm91cHMucHVzaChnZyk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHBlYyA9IGZlYXR1cmUuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ1BBUlQnKTtcbiAgICAgICAgICAgICAgICAgICAgaWYgKHBlYy5sZW5ndGggPiAwKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgcGFydHMgPSBbXTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGZvciAodmFyIHBpID0gMDsgcGkgPCBwZWMubGVuZ3RoOyArK3BpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcGFydHMucHVzaChwZWNbcGldLmdldEF0dHJpYnV0ZSgnaWQnKSk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLnBhcnRzID0gcGFydHM7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICB2YXIgcGVjID0gZmVhdHVyZS5nZXRFbGVtZW50c0J5VGFnTmFtZSgnUEFSRU5UJyk7XG4gICAgICAgICAgICAgICAgICAgIGlmIChwZWMubGVuZ3RoID4gMCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIHBhcmVudHMgPSBbXTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGZvciAodmFyIHBpID0gMDsgcGkgPCBwZWMubGVuZ3RoOyArK3BpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcGFyZW50cy5wdXNoKHBlY1twaV0uZ2V0QXR0cmlidXRlKCdpZCcpKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmUucGFyZW50cyA9IHBhcmVudHM7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgZmVhdHVyZXMucHVzaChkYXNGZWF0dXJlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICAgICAgICAgIFxuICAgICAgICBjYWxsYmFjayhmZWF0dXJlcywgdW5kZWZpbmVkLCBzZWdtZW50TWFwKTtcbiAgICB9LFxuICAgIGZ1bmN0aW9uIChlcnIpIHtcbiAgICAgICAgY2FsbGJhY2soW10sIGVycik7XG4gICAgfSk7XG59XG5cbmZ1bmN0aW9uIERBU0FsaWdubWVudCh0eXBlKSB7XG4gICAgdGhpcy50eXBlID0gdHlwZTtcbiAgICB0aGlzLm9iamVjdHMgPSB7fTtcbiAgICB0aGlzLmJsb2NrcyA9IFtdO1xufVxuXG5EQVNTb3VyY2UucHJvdG90eXBlLmFsaWdubWVudHMgPSBmdW5jdGlvbihzZWdtZW50LCBvcHRpb25zLCBjYWxsYmFjaykge1xuICAgIHZhciBkYXNVUkkgPSB0aGlzLmRhc0Jhc2VVUkkgKyAnYWxpZ25tZW50P3F1ZXJ5PScgKyBzZWdtZW50O1xuICAgIHRoaXMuZG9Dcm9zc0RvbWFpblJlcXVlc3QoZGFzVVJJLCBmdW5jdGlvbihyZXNwb25zZVhNTCkge1xuICAgICAgICBpZiAoIXJlc3BvbnNlWE1MKSB7XG4gICAgICAgICAgICBjYWxsYmFjayhbXSwgJ0ZhaWxlZCByZXF1ZXN0ICcgKyBkYXNVUkkpO1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG5cbiAgICAgICAgdmFyIGFsaWdubWVudHMgPSBbXTtcbiAgICAgICAgdmFyIGFsaVhNTHMgPSByZXNwb25zZVhNTC5nZXRFbGVtZW50c0J5VGFnTmFtZSgnYWxpZ25tZW50Jyk7XG4gICAgICAgIGZvciAodmFyIGFpID0gMDsgYWkgPCBhbGlYTUxzLmxlbmd0aDsgKythaSkge1xuICAgICAgICAgICAgdmFyIGFsaVhNTCA9IGFsaVhNTHNbYWldO1xuICAgICAgICAgICAgdmFyIGFsaSA9IG5ldyBEQVNBbGlnbm1lbnQoYWxpWE1MLmdldEF0dHJpYnV0ZSgnYWxpZ25UeXBlJykpO1xuICAgICAgICAgICAgdmFyIG9ialhNTHMgPSBhbGlYTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ2FsaWduT2JqZWN0Jyk7XG4gICAgICAgICAgICBmb3IgKHZhciBvaSA9IDA7IG9pIDwgb2JqWE1Mcy5sZW5ndGg7ICsrb2kpIHtcbiAgICAgICAgICAgICAgICB2YXIgb2JqWE1MID0gb2JqWE1Mc1tvaV07XG4gICAgICAgICAgICAgICAgdmFyIG9iaiA9IHtcbiAgICAgICAgICAgICAgICAgICAgaWQ6ICAgICAgICAgIG9ialhNTC5nZXRBdHRyaWJ1dGUoJ2ludE9iamVjdElkJyksXG4gICAgICAgICAgICAgICAgICAgIGFjY2Vzc2lvbjogICBvYmpYTUwuZ2V0QXR0cmlidXRlKCdkYkFjY2Vzc2lvbklkJyksXG4gICAgICAgICAgICAgICAgICAgIHZlcnNpb246ICAgICBvYmpYTUwuZ2V0QXR0cmlidXRlKCdvYmplY3RWZXJzaW9uJyksXG4gICAgICAgICAgICAgICAgICAgIGRiU291cmNlOiAgICBvYmpYTUwuZ2V0QXR0cmlidXRlKCdkYlNvdXJjZScpLFxuICAgICAgICAgICAgICAgICAgICBkYlZlcnNpb246ICAgb2JqWE1MLmdldEF0dHJpYnV0ZSgnZGJWZXJzaW9uJylcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIGFsaS5vYmplY3RzW29iai5pZF0gPSBvYmo7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBibG9ja1hNTHMgPSBhbGlYTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ2Jsb2NrJyk7XG4gICAgICAgICAgICBmb3IgKHZhciBiaSA9IDA7IGJpIDwgYmxvY2tYTUxzLmxlbmd0aDsgKytiaSkge1xuICAgICAgICAgICAgICAgIHZhciBibG9ja1hNTCA9IGJsb2NrWE1Mc1tiaV07XG4gICAgICAgICAgICAgICAgdmFyIGJsb2NrID0ge1xuICAgICAgICAgICAgICAgICAgICBvcmRlcjogICAgICBibG9ja1hNTC5nZXRBdHRyaWJ1dGUoJ2Jsb2NrT3JkZXInKSxcbiAgICAgICAgICAgICAgICAgICAgc2VnbWVudHM6ICAgW11cbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIHZhciBzZWdYTUxzID0gYmxvY2tYTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ3NlZ21lbnQnKTtcbiAgICAgICAgICAgICAgICBmb3IgKHZhciBzaSA9IDA7IHNpIDwgc2VnWE1Mcy5sZW5ndGg7ICsrc2kpIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHNlZ1hNTCA9IHNlZ1hNTHNbc2ldO1xuICAgICAgICAgICAgICAgICAgICB2YXIgc2VnID0ge1xuICAgICAgICAgICAgICAgICAgICAgICAgb2JqZWN0OiAgICAgIHNlZ1hNTC5nZXRBdHRyaWJ1dGUoJ2ludE9iamVjdElkJyksXG4gICAgICAgICAgICAgICAgICAgICAgICBtaW46ICAgICAgICAgc2VnWE1MLmdldEF0dHJpYnV0ZSgnc3RhcnQnKSxcbiAgICAgICAgICAgICAgICAgICAgICAgIG1heDogICAgICAgICBzZWdYTUwuZ2V0QXR0cmlidXRlKCdlbmQnKSxcbiAgICAgICAgICAgICAgICAgICAgICAgIHN0cmFuZDogICAgICBzZWdYTUwuZ2V0QXR0cmlidXRlKCdzdHJhbmQnKSxcbiAgICAgICAgICAgICAgICAgICAgICAgIGNpZ2FyOiAgICAgICBlbGVtZW50VmFsdWUoc2VnWE1MLCAnY2lnYXInKVxuICAgICAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgICAgICBibG9jay5zZWdtZW50cy5wdXNoKHNlZyk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGFsaS5ibG9ja3MucHVzaChibG9jayk7XG4gICAgICAgICAgICB9ICAgICAgIFxuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgIGFsaWdubWVudHMucHVzaChhbGkpO1xuICAgICAgICB9XG4gICAgICAgIGNhbGxiYWNrKGFsaWdubWVudHMpO1xuICAgIH0pO1xufVxuXG5cbmZ1bmN0aW9uIERBU1N0eWxlc2hlZXQoKSB7XG4vKlxuICAgIHRoaXMuaGlnaFpvb21TdHlsZXMgPSBuZXcgT2JqZWN0KCk7XG4gICAgdGhpcy5tZWRpdW1ab29tU3R5bGVzID0gbmV3IE9iamVjdCgpO1xuICAgIHRoaXMubG93Wm9vbVN0eWxlcyA9IG5ldyBPYmplY3QoKTtcbiovXG5cbiAgICB0aGlzLnN0eWxlcyA9IFtdO1xufVxuXG5EQVNTdHlsZXNoZWV0LnByb3RvdHlwZS5wdXNoU3R5bGUgPSBmdW5jdGlvbihmaWx0ZXJzLCB6b29tLCBzdHlsZSkge1xuICAgIC8qXG5cbiAgICBpZiAoIXpvb20pIHtcbiAgICAgICAgdGhpcy5oaWdoWm9vbVN0eWxlc1t0eXBlXSA9IHN0eWxlO1xuICAgICAgICB0aGlzLm1lZGl1bVpvb21TdHlsZXNbdHlwZV0gPSBzdHlsZTtcbiAgICAgICAgdGhpcy5sb3dab29tU3R5bGVzW3R5cGVdID0gc3R5bGU7XG4gICAgfSBlbHNlIGlmICh6b29tID09ICdoaWdoJykge1xuICAgICAgICB0aGlzLmhpZ2hab29tU3R5bGVzW3R5cGVdID0gc3R5bGU7XG4gICAgfSBlbHNlIGlmICh6b29tID09ICdtZWRpdW0nKSB7XG4gICAgICAgIHRoaXMubWVkaXVtWm9vbVN0eWxlc1t0eXBlXSA9IHN0eWxlO1xuICAgIH0gZWxzZSBpZiAoem9vbSA9PSAnbG93Jykge1xuICAgICAgICB0aGlzLmxvd1pvb21TdHlsZXNbdHlwZV0gPSBzdHlsZTtcbiAgICB9XG5cbiAgICAqL1xuXG4gICAgaWYgKCFmaWx0ZXJzKSB7XG4gICAgICAgIGZpbHRlcnMgPSB7dHlwZTogJ2RlZmF1bHQnfTtcbiAgICB9XG4gICAgdmFyIHN0eWxlSG9sZGVyID0gc2hhbGxvd0NvcHkoZmlsdGVycyk7XG4gICAgaWYgKHpvb20pIHtcbiAgICAgICAgc3R5bGVIb2xkZXIuem9vbSA9IHpvb207XG4gICAgfVxuICAgIHN0eWxlSG9sZGVyLnN0eWxlID0gc3R5bGU7XG4gICAgdGhpcy5zdHlsZXMucHVzaChzdHlsZUhvbGRlcik7XG59XG5cbmZ1bmN0aW9uIERBU1N0eWxlKCkge1xufVxuXG5mdW5jdGlvbiBwYXJzZUdyYWRpZW50KGdyYWQpIHtcbiAgICB2YXIgc3RlcHMgPSBncmFkLmdldEF0dHJpYnV0ZSgnc3RlcHMnKTtcbiAgICBpZiAoc3RlcHMpIHtcbiAgICAgICAgc3RlcHMgPSBzdGVwc3wwO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHN0ZXBzID0gNTA7XG4gICAgfVxuXG5cbiAgICB2YXIgc3RvcHMgPSBbXTtcbiAgICB2YXIgY29sb3JzID0gW107XG4gICAgdmFyIHNlID0gZ3JhZC5nZXRFbGVtZW50c0J5VGFnTmFtZSgnU1RPUCcpO1xuICAgIGZvciAodmFyIHNpID0gMDsgc2kgPCBzZS5sZW5ndGg7ICsrc2kpIHtcbiAgICAgICAgdmFyIHN0b3AgPSBzZVtzaV07XG4gICAgICAgIHN0b3BzLnB1c2goMS4wICogc3RvcC5nZXRBdHRyaWJ1dGUoJ3Njb3JlJykpO1xuICAgICAgICBjb2xvcnMucHVzaChzdG9wLmZpcnN0Q2hpbGQubm9kZVZhbHVlKTtcbiAgICB9XG5cbiAgICByZXR1cm4gbWFrZUNvbG91clN0ZXBzKHN0ZXBzLCBzdG9wcywgY29sb3JzKTtcbn1cblxuREFTU291cmNlLnByb3RvdHlwZS5zdHlsZXNoZWV0ID0gZnVuY3Rpb24oc3VjY2Vzc0NCLCBmYWlsdXJlQ0IpIHtcbiAgICB2YXIgZGFzVVJJLCBjcmVkcyA9IHRoaXMuY3JlZGVudGlhbHM7XG4gICAgaWYgKHRoaXMuc3R5bGVzaGVldF91cmkpIHtcbiAgICAgICAgZGFzVVJJID0gdGhpcy5zdHlsZXNoZWV0X3VyaTtcbiAgICAgICAgY3JlZHMgPSBmYWxzZTtcbiAgICB9IGVsc2Uge1xuICAgICAgICBkYXNVUkkgPSB0aGlzLmRhc0Jhc2VVUkkgKyAnc3R5bGVzaGVldCc7XG4gICAgfVxuXG4gICAgZG9Dcm9zc0RvbWFpblJlcXVlc3QoZGFzVVJJLCBmdW5jdGlvbihyZXNwb25zZVhNTCkge1xuICAgICAgICBpZiAoIXJlc3BvbnNlWE1MKSB7XG4gICAgICAgICAgICBpZiAoZmFpbHVyZUNCKSB7XG4gICAgICAgICAgICAgICAgZmFpbHVyZUNCKCk7XG4gICAgICAgICAgICB9IFxuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIHZhciBzdHlsZXNoZWV0ID0gbmV3IERBU1N0eWxlc2hlZXQoKTtcbiAgICAgICAgdmFyIHR5cGVYTUxzID0gcmVzcG9uc2VYTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ1RZUEUnKTtcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCB0eXBlWE1Mcy5sZW5ndGg7ICsraSkge1xuICAgICAgICAgICAgdmFyIHR5cGVTdHlsZSA9IHR5cGVYTUxzW2ldO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICB2YXIgZmlsdGVyID0ge307XG4gICAgICAgICAgICBmaWx0ZXIudHlwZSA9IHR5cGVTdHlsZS5nZXRBdHRyaWJ1dGUoJ2lkJyk7IC8vIEFtIEkgcmlnaHQgaW4gdGhpbmtpbmcgdGhhdCB0aGlzIG1ha2VzIERBU1NUWUxFIFhNTCBpbnZhbGlkPyAgVWdoLlxuICAgICAgICAgICAgZmlsdGVyLmxhYmVsID0gdHlwZVN0eWxlLmdldEF0dHJpYnV0ZSgnbGFiZWwnKTtcbiAgICAgICAgICAgIGZpbHRlci5tZXRob2QgPSB0eXBlU3R5bGUuZ2V0QXR0cmlidXRlKCdtZXRob2QnKTtcbiAgICAgICAgICAgIHZhciBnbHlwaFhNTHMgPSB0eXBlU3R5bGUuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ0dMWVBIJyk7XG4gICAgICAgICAgICBmb3IgKHZhciBnaSA9IDA7IGdpIDwgZ2x5cGhYTUxzLmxlbmd0aDsgKytnaSkge1xuICAgICAgICAgICAgICAgIHZhciBnbHlwaFhNTCA9IGdseXBoWE1Mc1tnaV07XG4gICAgICAgICAgICAgICAgdmFyIHpvb20gPSBnbHlwaFhNTC5nZXRBdHRyaWJ1dGUoJ3pvb20nKTtcbiAgICAgICAgICAgICAgICB2YXIgZ2x5cGggPSBjaGlsZEVsZW1lbnRPZihnbHlwaFhNTCk7XG4gICAgICAgICAgICAgICAgdmFyIHN0eWxlID0gbmV3IERBU1N0eWxlKCk7XG4gICAgICAgICAgICAgICAgc3R5bGUuZ2x5cGggPSBnbHlwaC5sb2NhbE5hbWU7XG4gICAgICAgICAgICAgICAgdmFyIGNoaWxkID0gZ2x5cGguZmlyc3RDaGlsZDtcbiAgICAgICAgXG4gICAgICAgICAgICAgICAgd2hpbGUgKGNoaWxkKSB7XG4gICAgICAgICAgICAgICAgICAgIGlmIChjaGlsZC5ub2RlVHlwZSA9PSBOb2RlLkVMRU1FTlRfTk9ERSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgLy8gYWxlcnQoY2hpbGQubG9jYWxOYW1lKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmIChjaGlsZC5sb2NhbE5hbWUgPT0gJ0JHR1JBRCcpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBzdHlsZVtjaGlsZC5sb2NhbE5hbWVdID0gcGFyc2VHcmFkaWVudChjaGlsZCk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9IGVsc2UgeyAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHN0eWxlW2NoaWxkLmxvY2FsTmFtZV0gPSBjaGlsZC5maXJzdENoaWxkLm5vZGVWYWx1ZTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBjaGlsZCA9IGNoaWxkLm5leHRTaWJsaW5nO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBzdHlsZXNoZWV0LnB1c2hTdHlsZShmaWx0ZXIsIHpvb20sIHN0eWxlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBzdWNjZXNzQ0Ioc3R5bGVzaGVldCk7XG4gICAgfSwgY3JlZHMpO1xufVxuXG4vL1xuLy8gc291cmNlcyBjb21tYW5kXG4vLyBcblxuZnVuY3Rpb24gREFTUmVnaXN0cnkodXJpLCBvcHRzKVxue1xuICAgIG9wdHMgPSBvcHRzIHx8IHt9O1xuICAgIHRoaXMudXJpID0gdXJpO1xuICAgIHRoaXMub3B0cyA9IG9wdHM7ICAgXG59XG5cbkRBU1JlZ2lzdHJ5LnByb3RvdHlwZS5zb3VyY2VzID0gZnVuY3Rpb24oY2FsbGJhY2ssIGZhaWx1cmUsIG9wdHMpXG57XG4gICAgaWYgKCFvcHRzKSB7XG4gICAgICAgIG9wdHMgPSB7fTtcbiAgICB9XG5cbiAgICB2YXIgZmlsdGVycyA9IFtdO1xuICAgIGlmIChvcHRzLnRheG9uKSB7XG4gICAgICAgIGZpbHRlcnMucHVzaCgnb3JnYW5pc209JyArIG9wdHMudGF4b24pO1xuICAgIH1cbiAgICBpZiAob3B0cy5hdXRoKSB7XG4gICAgICAgIGZpbHRlcnMucHVzaCgnYXV0aG9yaXR5PScgKyBvcHRzLmF1dGgpO1xuICAgIH1cbiAgICBpZiAob3B0cy52ZXJzaW9uKSB7XG4gICAgICAgIGZpbHRlcnMucHVzaCgndmVyc2lvbj0nICsgb3B0cy52ZXJzaW9uKTtcbiAgICB9XG4gICAgdmFyIHF1cmkgPSB0aGlzLnVyaTtcbiAgICBpZiAoZmlsdGVycy5sZW5ndGggPiAwKSB7XG4gICAgICAgIHF1cmkgPSBxdXJpICsgJz8nICsgZmlsdGVycy5qb2luKCcmJyk7ICAgLy8gJyYnIGFzIGEgc2VwYXJhdG9yIHRvIGhhY2sgYXJvdW5kIGRhc3JlZ2lzdHJ5Lm9yZyBidWcuXG4gICAgfVxuXG4gICAgZG9Dcm9zc0RvbWFpblJlcXVlc3QocXVyaSwgZnVuY3Rpb24ocmVzcG9uc2VYTUwpIHtcbiAgICAgICAgaWYgKCFyZXNwb25zZVhNTCAmJiBmYWlsdXJlKSB7XG4gICAgICAgICAgICBmYWlsdXJlKCk7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cblxuICAgICAgICB2YXIgc291cmNlcyA9IFtdOyAgICAgICBcbiAgICAgICAgdmFyIHNvdXJjZVhNTHMgPSByZXNwb25zZVhNTC5nZXRFbGVtZW50c0J5VGFnTmFtZSgnU09VUkNFJyk7XG4gICAgICAgIGZvciAodmFyIHNpID0gMDsgc2kgPCBzb3VyY2VYTUxzLmxlbmd0aDsgKytzaSkge1xuICAgICAgICAgICAgdmFyIHNvdXJjZVhNTCA9IHNvdXJjZVhNTHNbc2ldO1xuICAgICAgICAgICAgdmFyIHZlcnNpb25YTUxzID0gc291cmNlWE1MLmdldEVsZW1lbnRzQnlUYWdOYW1lKCdWRVJTSU9OJyk7XG4gICAgICAgICAgICBpZiAodmVyc2lvblhNTHMubGVuZ3RoIDwgMSkge1xuICAgICAgICAgICAgICAgIGNvbnRpbnVlO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdmFyIHZlcnNpb25YTUwgPSB2ZXJzaW9uWE1Mc1swXTtcblxuICAgICAgICAgICAgdmFyIGNvb3JkWE1McyA9IHZlcnNpb25YTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ0NPT1JESU5BVEVTJyk7XG4gICAgICAgICAgICB2YXIgY29vcmRzID0gW107XG4gICAgICAgICAgICBmb3IgKHZhciBjaSA9IDA7IGNpIDwgY29vcmRYTUxzLmxlbmd0aDsgKytjaSkge1xuICAgICAgICAgICAgICAgIHZhciBjb29yZFhNTCA9IGNvb3JkWE1Mc1tjaV07XG4gICAgICAgICAgICAgICAgdmFyIGNvb3JkID0gbmV3IERBU0Nvb3JkcygpO1xuICAgICAgICAgICAgICAgIGNvb3JkLmF1dGggPSBjb29yZFhNTC5nZXRBdHRyaWJ1dGUoJ2F1dGhvcml0eScpO1xuICAgICAgICAgICAgICAgIGNvb3JkLnRheG9uID0gY29vcmRYTUwuZ2V0QXR0cmlidXRlKCd0YXhpZCcpO1xuICAgICAgICAgICAgICAgIGNvb3JkLnZlcnNpb24gPSBjb29yZFhNTC5nZXRBdHRyaWJ1dGUoJ3ZlcnNpb24nKTtcbiAgICAgICAgICAgICAgICBjb29yZHMucHVzaChjb29yZCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBjYXBzID0gW107XG4gICAgICAgICAgICB2YXIgY2FwWE1McyA9IHZlcnNpb25YTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ0NBUEFCSUxJVFknKTtcbiAgICAgICAgICAgIHZhciB1cmk7XG4gICAgICAgICAgICBmb3IgKHZhciBjaSA9IDA7IGNpIDwgY2FwWE1Mcy5sZW5ndGg7ICsrY2kpIHtcbiAgICAgICAgICAgICAgICB2YXIgY2FwWE1MID0gY2FwWE1Mc1tjaV07XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgY2Fwcy5wdXNoKGNhcFhNTC5nZXRBdHRyaWJ1dGUoJ3R5cGUnKSk7XG5cbiAgICAgICAgICAgICAgICBpZiAoY2FwWE1MLmdldEF0dHJpYnV0ZSgndHlwZScpID09ICdkYXMxOmZlYXR1cmVzJykge1xuICAgICAgICAgICAgICAgICAgICB2YXIgZmVwID0gY2FwWE1MLmdldEF0dHJpYnV0ZSgncXVlcnlfdXJpJyk7XG4gICAgICAgICAgICAgICAgICAgIHVyaSA9IGZlcC5zdWJzdHJpbmcoMCwgZmVwLmxlbmd0aCAtICgnZmVhdHVyZXMnLmxlbmd0aCkpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgdmFyIHByb3BzID0ge307XG4gICAgICAgICAgICB2YXIgcHJvcFhNTHMgPSB2ZXJzaW9uWE1MLmdldEVsZW1lbnRzQnlUYWdOYW1lKCdQUk9QJyk7XG4gICAgICAgICAgICBmb3IgKHZhciBwaSA9IDA7IHBpIDwgcHJvcFhNTHMubGVuZ3RoOyArK3BpKSB7XG4gICAgICAgICAgICAgICAgcHVzaG8ocHJvcHMsIHByb3BYTUxzW3BpXS5nZXRBdHRyaWJ1dGUoJ25hbWUnKSwgcHJvcFhNTHNbcGldLmdldEF0dHJpYnV0ZSgndmFsdWUnKSk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICh1cmkpIHtcbiAgICAgICAgICAgICAgICB2YXIgc291cmNlID0gbmV3IERBU1NvdXJjZSh1cmksIHtcbiAgICAgICAgICAgICAgICAgICAgc291cmNlX3VyaTogc291cmNlWE1MLmdldEF0dHJpYnV0ZSgndXJpJyksXG4gICAgICAgICAgICAgICAgICAgIG5hbWU6ICBzb3VyY2VYTUwuZ2V0QXR0cmlidXRlKCd0aXRsZScpLFxuICAgICAgICAgICAgICAgICAgICBkZXNjOiAgc291cmNlWE1MLmdldEF0dHJpYnV0ZSgnZGVzY3JpcHRpb24nKSxcbiAgICAgICAgICAgICAgICAgICAgY29vcmRzOiBjb29yZHMsXG4gICAgICAgICAgICAgICAgICAgIHByb3BzOiBwcm9wcyxcbiAgICAgICAgICAgICAgICAgICAgY2FwYWJpbGl0aWVzOiBjYXBzXG4gICAgICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICAgICAgc291cmNlcy5wdXNoKHNvdXJjZSk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIGNhbGxiYWNrKHNvdXJjZXMpO1xuICAgIH0pO1xufVxuXG5cbi8vXG4vLyBVdGlsaXR5IGZ1bmN0aW9uc1xuLy9cblxuZnVuY3Rpb24gZWxlbWVudFZhbHVlKGVsZW1lbnQsIHRhZylcbntcbiAgICB2YXIgY2hpbGRyZW4gPSBlbGVtZW50LmdldEVsZW1lbnRzQnlUYWdOYW1lKHRhZyk7XG4gICAgaWYgKGNoaWxkcmVuLmxlbmd0aCA+IDAgJiYgY2hpbGRyZW5bMF0uZmlyc3RDaGlsZCkge1xuICAgICAgICB2YXIgYyA9IGNoaWxkcmVuWzBdO1xuICAgICAgICBpZiAoYy5jaGlsZE5vZGVzLmxlbmd0aCA9PSAxKSB7XG4gICAgICAgICAgICByZXR1cm4gYy5maXJzdENoaWxkLm5vZGVWYWx1ZTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIHZhciBzID0gJyc7XG4gICAgICAgICAgICBmb3IgKHZhciBuaSA9IDA7IG5pIDwgYy5jaGlsZE5vZGVzLmxlbmd0aDsgKytuaSkge1xuICAgICAgICAgICAgICAgIHMgKz0gYy5jaGlsZE5vZGVzW25pXS5ub2RlVmFsdWU7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZXR1cm4gcztcbiAgICAgICAgfVxuXG4gICAgfSBlbHNlIHtcbiAgICAgICAgcmV0dXJuIG51bGw7XG4gICAgfVxufVxuXG5mdW5jdGlvbiBjaGlsZEVsZW1lbnRPZihlbGVtZW50KVxue1xuICAgIGlmIChlbGVtZW50Lmhhc0NoaWxkTm9kZXMoKSkge1xuICAgICAgICB2YXIgY2hpbGQgPSBlbGVtZW50LmZpcnN0Q2hpbGQ7XG4gICAgICAgIGRvIHtcbiAgICAgICAgICAgIGlmIChjaGlsZC5ub2RlVHlwZSA9PSBOb2RlLkVMRU1FTlRfTk9ERSkge1xuICAgICAgICAgICAgICAgIHJldHVybiBjaGlsZDtcbiAgICAgICAgICAgIH0gXG4gICAgICAgICAgICBjaGlsZCA9IGNoaWxkLm5leHRTaWJsaW5nO1xuICAgICAgICB9IHdoaWxlIChjaGlsZCAhPSBudWxsKTtcbiAgICB9XG4gICAgcmV0dXJuIG51bGw7XG59XG5cblxuZnVuY3Rpb24gZGFzTGlua3NPZihlbGVtZW50KVxue1xuICAgIHZhciBsaW5rcyA9IG5ldyBBcnJheSgpO1xuICAgIHZhciBtYXliZUxpbmtDaGlsZGVuID0gZWxlbWVudC5nZXRFbGVtZW50c0J5VGFnTmFtZSgnTElOSycpO1xuICAgIGZvciAodmFyIGNpID0gMDsgY2kgPCBtYXliZUxpbmtDaGlsZGVuLmxlbmd0aDsgKytjaSkge1xuICAgICAgICB2YXIgbGlua1hNTCA9IG1heWJlTGlua0NoaWxkZW5bY2ldO1xuICAgICAgICBpZiAobGlua1hNTC5wYXJlbnROb2RlID09IGVsZW1lbnQpIHtcbiAgICAgICAgICAgIGxpbmtzLnB1c2gobmV3IERBU0xpbmsobGlua1hNTC5maXJzdENoaWxkID8gbGlua1hNTC5maXJzdENoaWxkLm5vZGVWYWx1ZSA6ICdVbmtub3duJywgbGlua1hNTC5nZXRBdHRyaWJ1dGUoJ2hyZWYnKSkpO1xuICAgICAgICB9XG4gICAgfVxuICAgIFxuICAgIHJldHVybiBsaW5rcztcbn1cblxuZnVuY3Rpb24gZGFzTm90ZXNPZihlbGVtZW50KVxue1xuICAgIHZhciBub3RlcyA9IFtdO1xuICAgIHZhciBtYXliZU5vdGVzID0gZWxlbWVudC5nZXRFbGVtZW50c0J5VGFnTmFtZSgnTk9URScpO1xuICAgIGZvciAodmFyIG5pID0gMDsgbmkgPCBtYXliZU5vdGVzLmxlbmd0aDsgKytuaSkge1xuICAgICAgICBpZiAobWF5YmVOb3Rlc1tuaV0uZmlyc3RDaGlsZCkge1xuICAgICAgICAgICAgbm90ZXMucHVzaChtYXliZU5vdGVzW25pXS5maXJzdENoaWxkLm5vZGVWYWx1ZSk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgcmV0dXJuIG5vdGVzO1xufVxuXG5mdW5jdGlvbiBkb0Nyb3NzRG9tYWluUmVxdWVzdCh1cmwsIGhhbmRsZXIsIGNyZWRlbnRpYWxzLCBjdXN0QXV0aCkge1xuICAgIC8vIFRPRE86IGV4cGxpY2l0IGVycm9yIGhhbmRsZXJzP1xuXG4gICAgaWYgKHdpbmRvdy5YRG9tYWluUmVxdWVzdCkge1xuICAgICAgICB2YXIgcmVxID0gbmV3IFhEb21haW5SZXF1ZXN0KCk7XG4gICAgICAgIHJlcS5vbmxvYWQgPSBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIHZhciBkb20gPSBuZXcgQWN0aXZlWE9iamVjdChcIk1pY3Jvc29mdC5YTUxET01cIik7XG4gICAgICAgICAgICBkb20uYXN5bmMgPSBmYWxzZTtcbiAgICAgICAgICAgIGRvbS5sb2FkWE1MKHJlcS5yZXNwb25zZVRleHQpO1xuICAgICAgICAgICAgaGFuZGxlcihkb20pO1xuICAgICAgICB9XG4gICAgICAgIHJlcS5vcGVuKFwiZ2V0XCIsIHVybCk7XG4gICAgICAgIHJlcS5zZW5kKCcnKTtcbiAgICB9IGVsc2Uge1xuICAgICAgICB2YXIgcmVxU3RhcnQgPSBEYXRlLm5vdygpO1xuICAgICAgICB2YXIgcmVxID0gbmV3IFhNTEh0dHBSZXF1ZXN0KCk7XG5cbiAgICAgICAgcmVxLm9ucmVhZHlzdGF0ZWNoYW5nZSA9IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgaWYgKHJlcS5yZWFkeVN0YXRlID09IDQpIHtcbiAgICAgICAgICAgICAgaWYgKHJlcS5zdGF0dXMgPj0gMjAwIHx8IHJlcS5zdGF0dXMgPT0gMCkge1xuICAgICAgICAgICAgICAgICAgaGFuZGxlcihyZXEucmVzcG9uc2VYTUwsIHJlcSk7XG4gICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfTtcbiAgICAgICAgcmVxLm9wZW4oXCJnZXRcIiwgdXJsLCB0cnVlKTtcbiAgICAgICAgaWYgKGNyZWRlbnRpYWxzKSB7XG4gICAgICAgICAgICByZXEud2l0aENyZWRlbnRpYWxzID0gdHJ1ZTtcbiAgICAgICAgfVxuICAgICAgICBpZiAoY3VzdEF1dGgpIHtcbiAgICAgICAgICAgIHJlcS5zZXRSZXF1ZXN0SGVhZGVyKCdYLURBUy1BdXRob3Jpc2F0aW9uJywgY3VzdEF1dGgpO1xuICAgICAgICB9XG4gICAgICAgIHJlcS5zZXRSZXF1ZXN0SGVhZGVyKCdBY2NlcHQnLCAnYXBwbGljYXRpb24veG1sLCovKicpO1xuICAgICAgICByZXEuc2VuZCgnJyk7XG4gICAgfVxufVxuXG5EQVNTb3VyY2UucHJvdG90eXBlLmRvQ3Jvc3NEb21haW5SZXF1ZXN0ID0gZnVuY3Rpb24odXJsLCBoYW5kbGVyLCBlcnJIYW5kbGVyKSB7XG4gICAgdmFyIGN1c3RBdXRoO1xuICAgIGlmICh0aGlzLnhVc2VyKSB7XG4gICAgICAgIGN1c3RBdXRoID0gJ0Jhc2ljICcgKyBidG9hKHRoaXMueFVzZXIgKyAnOicgKyB0aGlzLnhQYXNzKTtcbiAgICB9XG5cbiAgICB0cnkge1xuICAgICAgICByZXR1cm4gZG9Dcm9zc0RvbWFpblJlcXVlc3QodXJsLCBoYW5kbGVyLCB0aGlzLmNyZWRlbnRpYWxzLCBjdXN0QXV0aCk7XG4gICAgfSBjYXRjaCAoZXJyKSB7XG4gICAgICAgIGlmIChlcnJIYW5kbGVyKSB7XG4gICAgICAgICAgICBlcnJIYW5kbGVyKGVycik7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICB0aHJvdyBlcnI7XG4gICAgICAgIH1cbiAgICB9XG59XG5cbmZ1bmN0aW9uIGlzRGFzQm9vbGVhblRydWUocykge1xuICAgIHMgPSAoJycgKyBzKS50b0xvd2VyQ2FzZSgpO1xuICAgIHJldHVybiBzPT09J3llcycgfHwgcz09PSd0cnVlJztcbn1cblxuZnVuY3Rpb24gaXNEYXNCb29sZWFuTm90RmFsc2Uocykge1xuICAgIGlmICghcylcbiAgICAgICAgcmV0dXJuIGZhbHNlO1xuXG4gICAgcyA9ICgnJyArIHMpLnRvTG93ZXJDYXNlKCk7XG4gICAgcmV0dXJuIHMhPT0nbm8nIHx8IHMhPT0nZmFsc2UnO1xufVxuXG5mdW5jdGlvbiBjb3B5U3R5bGVzaGVldChzcykge1xuICAgIHZhciBuc3MgPSBzaGFsbG93Q29weShzcyk7XG4gICAgbnNzLnN0eWxlcyA9IFtdO1xuICAgIGZvciAodmFyIHNpID0gMDsgc2kgPCBzcy5zdHlsZXMubGVuZ3RoOyArK3NpKSB7XG4gICAgICAgIHZhciBzaCA9IG5zcy5zdHlsZXNbc2ldID0gc2hhbGxvd0NvcHkoc3Muc3R5bGVzW3NpXSk7XG4gICAgICAgIHNoLl9tZXRob2RSRSA9IHNoLl9sYWJlbFJFID0gc2guX3R5cGVSRSA9IHVuZGVmaW5lZDtcbiAgICAgICAgc2guc3R5bGUgPSBzaGFsbG93Q29weShzaC5zdHlsZSk7XG4gICAgICAgIHNoLnN0eWxlLmlkID0gdW5kZWZpbmVkO1xuICAgIH1cbiAgICByZXR1cm4gbnNzO1xufVxuXG5pZiAodHlwZW9mKG1vZHVsZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgbW9kdWxlLmV4cG9ydHMgPSB7XG4gICAgICAgIERBU0dyb3VwOiBEQVNHcm91cCxcbiAgICAgICAgREFTRmVhdHVyZTogREFTRmVhdHVyZSxcbiAgICAgICAgREFTU3R5bGVzaGVldDogREFTU3R5bGVzaGVldCxcbiAgICAgICAgREFTU3R5bGU6IERBU1N0eWxlLFxuICAgICAgICBEQVNTb3VyY2U6IERBU1NvdXJjZSxcbiAgICAgICAgREFTU2VnbWVudDogREFTU2VnbWVudCxcbiAgICAgICAgREFTUmVnaXN0cnk6IERBU1JlZ2lzdHJ5LFxuICAgICAgICBEQVNTZXF1ZW5jZTogREFTU2VxdWVuY2UsXG5cbiAgICAgICAgaXNEYXNCb29sZWFuVHJ1ZTogaXNEYXNCb29sZWFuVHJ1ZSxcbiAgICAgICAgaXNEYXNCb29sZWFuTm90RmFsc2U6IGlzRGFzQm9vbGVhbk5vdEZhbHNlLFxuICAgICAgICBjb3B5U3R5bGVzaGVldDogY29weVN0eWxlc2hlZXRcbiAgICB9O1xufSIsIihmdW5jdGlvbiAoZ2xvYmFsKXtcbi8qIC0qLSBtb2RlOiBqYXZhc2NyaXB0OyBjLWJhc2ljLW9mZnNldDogNDsgaW5kZW50LXRhYnMtbW9kZTogbmlsIC0qLSAqL1xuXG4vLyBcbi8vIERhbGxpYW5jZSBHZW5vbWUgRXhwbG9yZXJcbi8vIChjKSBUaG9tYXMgRG93biAyMDA2LTIwMTRcbi8vXG4vLyBmZXRjaHdvcmtlci5qc1xuLy9cblxuXCJ1c2Ugc3RyaWN0XCI7XG5cbnZhciBiaW4gPSByZXF1aXJlKCcuL2JpbicpO1xudmFyIGJhbSA9IHJlcXVpcmUoJy4vYmFtJyk7XG52YXIgYmlnd2lnID0gcmVxdWlyZSgnLi9iaWd3aWcnKTtcblxudmFyIGNvbm5lY3Rpb25zID0ge307XG5cbnZhciBpZFNlZWQgPSAwO1xuXG5nbG9iYWwubmV3SUQgPSBmdW5jdGlvbigpIHtcbiAgICByZXR1cm4gJ2NuJyArICgrK2lkU2VlZCk7XG59XG5cbnBvc3RNZXNzYWdlKHt0YWc6ICdpbml0J30pO1xuXG5zZWxmLm9ubWVzc2FnZSA9IGZ1bmN0aW9uKGV2ZW50KSB7XG4gICAgdmFyIGQgPSBldmVudC5kYXRhO1xuICAgIHZhciBjb21tYW5kID0gZXZlbnQuZGF0YS5jb21tYW5kO1xuICAgIHZhciB0YWcgPSBldmVudC5kYXRhLnRhZztcblxuICAgIGlmIChjb21tYW5kID09PSAnY29ubmVjdEJBTScpIHtcbiAgICAgICAgdmFyIGlkID0gbmV3SUQoKTtcblxuICAgICAgICB2YXIgYmFtRiwgYmFpRjtcbiAgICAgICAgaWYgKGQuYmxvYikge1xuICAgICAgICAgICAgYmFtRiA9IG5ldyBiaW4uQmxvYkZldGNoYWJsZShkLmJsb2IpO1xuICAgICAgICAgICAgYmFpRiA9IG5ldyBiaW4uQmxvYkZldGNoYWJsZShkLmluZGV4QmxvYik7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBiYW1GID0gbmV3IGJpbi5VUkxGZXRjaGFibGUoZC51cmkpO1xuICAgICAgICAgICAgYmFpRiA9IG5ldyBiaW4uVVJMRmV0Y2hhYmxlKGQuaW5kZXhVcmkpO1xuICAgICAgICB9XG5cbiAgICAgICAgYmFtLm1ha2VCYW0oYmFtRiwgYmFpRiwgZnVuY3Rpb24oYmFtT2JqLCBlcnIpIHtcbiAgICAgICAgICAgIGlmIChiYW1PYmopIHtcbiAgICAgICAgICAgICAgICBjb25uZWN0aW9uc1tpZF0gPSBuZXcgQkFNV29ya2VyRmV0Y2hlcihiYW1PYmopO1xuICAgICAgICAgICAgICAgIHBvc3RNZXNzYWdlKHt0YWc6IHRhZywgcmVzdWx0OiBpZH0pO1xuICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIGVycm9yOiBlcnIgfHwgXCJDb3VsZG4ndCBmZXRjaCBCQU1cIn0pO1xuICAgICAgICAgICAgfVxuICAgICAgICB9KTtcbiAgICB9IGVsc2UgaWYgKGNvbW1hbmQgPT09ICdjb25uZWN0QkJJJykge1xuICAgICAgICB2YXIgaWQgPSBuZXdJRCgpO1xuICAgICAgICB2YXIgYmJpO1xuICAgICAgICBpZiAoZC5ibG9iKSB7XG4gICAgICAgICAgICBiYmkgPSBuZXcgYmluLkJsb2JGZXRjaGFibGUoZC5ibG9iKTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIGJiaSA9IG5ldyBiaW4uVVJMRmV0Y2hhYmxlKGQudXJpKTtcbiAgICAgICAgfVxuXG4gICAgICAgIGJpZ3dpZy5tYWtlQndnKGJiaSwgZnVuY3Rpb24oYndnLCBlcnIpIHtcbiAgICAgICAgICAgIGlmIChid2cpIHtcbiAgICAgICAgICAgICAgICBjb25uZWN0aW9uc1tpZF0gPSBuZXcgQkJJV29ya2VyRmV0Y2hlcihid2cpO1xuICAgICAgICAgICAgICAgIHBvc3RNZXNzYWdlKHt0YWc6IHRhZywgcmVzdWx0OiBpZH0pO1xuICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIGVycm9yOiBlcnIgfHwgXCJDb3VsZG4ndCBmZXRjaCBCQklcIn0pO1xuICAgICAgICAgICAgfVxuICAgICAgICB9LCBkLnVyaSk7XG4gICAgfSBlbHNlIGlmIChjb21tYW5kID09PSAnZmV0Y2gnKSB7XG4gICAgICAgIHZhciBjb24gPSBjb25uZWN0aW9uc1tldmVudC5kYXRhLmNvbm5lY3Rpb25dO1xuICAgICAgICBpZiAoIWNvbikge1xuICAgICAgICAgICAgcmV0dXJuIHBvc3RNZXNzYWdlKHt0YWc6IHRhZywgZXJyb3I6ICdObyBzdWNoIGNvbm5lY3Rpb246ICcgKyBldmVudC5kYXRhLmNvbm5lY3Rpb259KTtcbiAgICAgICAgfVxuXG4gICAgICAgIGNvbi5mZXRjaChkLnRhZywgZC5jaHIsIGQubWluLCBkLm1heCwgZC56b29tLCBkLm9wdHMpO1xuICAgIH0gZWxzZSBpZiAoY29tbWFuZCA9PT0gJ2xlYXAnKSB7XG4gICAgICAgIHZhciBjb24gPSBjb25uZWN0aW9uc1tldmVudC5kYXRhLmNvbm5lY3Rpb25dO1xuICAgICAgICBpZiAoIWNvbikge1xuICAgICAgICAgICAgcmV0dXJuIHBvc3RNZXNzYWdlKHt0YWc6IHRhZywgZXJyb3I6ICdObyBzdWNoIGNvbm5lY3Rpb246ICcgKyBldmVudC5kYXRhLmNvbm5lY3Rpb259KTtcbiAgICAgICAgfVxuXG4gICAgICAgIGNvbi5sZWFwKGQudGFnLCBkLmNociwgZC5wb3MsIGQuZGlyKTtcbiAgICB9IGVsc2UgaWYgKGNvbW1hbmQgPT09ICdxdWFudExlYXAnKSB7XG4gICAgICAgIHZhciBjb24gPSBjb25uZWN0aW9uc1tldmVudC5kYXRhLmNvbm5lY3Rpb25dO1xuICAgICAgICBpZiAoIWNvbikge1xuICAgICAgICAgICAgcmV0dXJuIHBvc3RNZXNzYWdlKHt0YWc6IHRhZywgZXJyb3I6ICdObyBzdWNoIGNvbm5lY3Rpb246ICcgKyBldmVudC5kYXRhLmNvbm5lY3Rpb259KTtcbiAgICAgICAgfVxuXG4gICAgICAgIGNvbi5xdWFudExlYXAoZC50YWcsIGQuY2hyLCBkLnBvcywgZC5kaXIsIGQudGhyZXNob2xkLCBkLnVuZGVyKTtcbiAgICB9IGVsc2UgaWYgKGNvbW1hbmQgPT09ICdtZXRhJykge1xuICAgICAgICB2YXIgY29uID0gY29ubmVjdGlvbnNbZXZlbnQuZGF0YS5jb25uZWN0aW9uXTtcbiAgICAgICAgaWYgKCFjb24pIHtcbiAgICAgICAgICAgIHJldHVybiBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIGVycm9yOiAnTm8gc3VjaCBjb25uZWN0aW9uOiAnICsgZXZlbnQuZGF0YS5jb25uZWN0aW9ufSk7XG4gICAgICAgIH1cblxuICAgICAgICBjb24ubWV0YShkLnRhZyk7XG4gICAgfSBlbHNlIGlmIChjb21tYW5kID09PSAnc2VhcmNoJykge1xuICAgICAgICB2YXIgY29uID0gY29ubmVjdGlvbnNbZXZlbnQuZGF0YS5jb25uZWN0aW9uXTtcbiAgICAgICAgaWYgKCFjb24pIHtcbiAgICAgICAgICAgIHJldHVybiBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIGVycm9yOiAnTm8gc3VjaCBjb25uZWN0aW9uOiAnICsgZXZlbnQuZGF0YS5jb25uZWN0aW9ufSk7XG4gICAgICAgIH1cblxuICAgICAgICBjb24uc2VhcmNoKGQudGFnLCBkLnF1ZXJ5LCBkLmluZGV4KTtcbiAgICB9IGVsc2UgaWYgKGNvbW1hbmQgPT09ICdkYXRlJykge1xuICAgICAgICByZXR1cm4gcG9zdE1lc3NhZ2Uoe3RhZzogdGFnLCByZXN1bHQ6IERhdGUubm93KCl8MH0pO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHBvc3RNZXNzYWdlKHt0YWc6IHRhZywgZXJyb3I6ICdCYWQgY29tbWFuZCAnICsgY29tbWFuZH0pO1xuICAgIH1cbn1cblxuZnVuY3Rpb24gQkFNV29ya2VyRmV0Y2hlcihiYW0pIHtcbiAgICB0aGlzLmJhbSA9IGJhbTtcbn1cblxuQkFNV29ya2VyRmV0Y2hlci5wcm90b3R5cGUuZmV0Y2ggPSBmdW5jdGlvbih0YWcsIGNociwgbWluLCBtYXgsIHpvb20sIG9wdHMpIHtcbiAgICBvcHRzID0gb3B0cyB8fCB7fTtcbiAgICB0aGlzLmJhbS5mZXRjaChjaHIsIG1pbiwgbWF4LCBmdW5jdGlvbihyZWNvcmRzLCBlcnIpIHtcbiAgICAgICAgaWYgKHJlY29yZHMpIHtcbiAgICAgICAgICAgIHBvc3RNZXNzYWdlKHt0YWc6IHRhZywgcmVzdWx0OiByZWNvcmRzLCB0aW1lOiBEYXRlLm5vdygpfDB9KTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIHBvc3RNZXNzYWdlKHt0YWc6IHRhZywgZXJyb3I6IGVycn0pO1xuICAgICAgICB9XG4gICAgfSwgb3B0cyk7XG59XG5cbmZ1bmN0aW9uIEJCSVdvcmtlckZldGNoZXIoYmJpKSB7XG4gICAgdGhpcy5iYmkgPSBiYmk7XG59XG5cbkJCSVdvcmtlckZldGNoZXIucHJvdG90eXBlLmZldGNoID0gZnVuY3Rpb24odGFnLCBjaHIsIG1pbiwgbWF4LCB6b29tKSB7XG4gICAgaWYgKHR5cGVvZih6b29tKSAhPT0gJ251bWJlcicpXG4gICAgICAgIHpvb20gPSAtMTtcblxuICAgIHZhciBkYXRhO1xuICAgIGlmICh6b29tIDwgMCkge1xuICAgICAgICBkYXRhID0gdGhpcy5iYmkuZ2V0VW56b29tZWRWaWV3KCk7XG4gICAgfSBlbHNlIHtcbiAgICAgICAgZGF0YSA9IHRoaXMuYmJpLmdldFpvb21lZFZpZXcoem9vbSk7XG4gICAgfVxuXG4gICAgZGF0YS5yZWFkV2lnRGF0YShjaHIsIG1pbiwgbWF4LCBmdW5jdGlvbihmZWF0dXJlcykge1xuICAgICAgICBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIHJlc3VsdDogZmVhdHVyZXN9KTtcbiAgICB9KTtcbn1cblxuQkJJV29ya2VyRmV0Y2hlci5wcm90b3R5cGUubWV0YSA9IGZ1bmN0aW9uKHRhZykge1xuICAgIHZhciBzY2FsZXMgPSBbMV07XG4gICAgZm9yICh2YXIgeiA9IDA7IHogPCB0aGlzLmJiaS56b29tTGV2ZWxzLmxlbmd0aDsgKyt6KSB7XG4gICAgICAgIHNjYWxlcy5wdXNoKHRoaXMuYmJpLnpvb21MZXZlbHNbel0ucmVkdWN0aW9uKTtcbiAgICB9XG5cbiAgICB2YXIgdGhpc0IgPSB0aGlzO1xuICAgIHZhciBtZXRhID0ge3R5cGU6IHRoaXMuYmJpLnR5cGUsXG4gICAgICAgICAgICAgICAgem9vbUxldmVsczogc2NhbGVzLFxuICAgICAgICAgICAgICAgIGZpZWxkQ291bnQ6IHRoaXMuYmJpLmZpZWxkQ291bnQsXG4gICAgICAgICAgICAgICAgZGVmaW5lZEZpZWxkQ291bnQ6IHRoaXMuYmJpLmRlZmluZWRGaWVsZENvdW50LFxuICAgICAgICAgICAgICAgIHNjaGVtYTogdGhpcy5iYmkuc2NoZW1hfTtcbiAgICBpZiAodGhpcy5iYmkudHlwZSA9PT0gJ2JpZ2JlZCcpIHtcbiAgICAgICAgdGhpcy5iYmkuZ2V0RXh0cmFJbmRpY2VzKGZ1bmN0aW9uKGVpKSB7XG4gICAgICAgICAgICBpZiAoZWkpIHtcbiAgICAgICAgICAgICAgICB0aGlzQi5leHRyYUluZGljZXMgPSBlaTtcbiAgICAgICAgICAgICAgICBtZXRhLmV4dHJhSW5kaWNlcyA9IGVpLm1hcChmdW5jdGlvbihpKSB7cmV0dXJuIGkuZmllbGR9KTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHBvc3RNZXNzYWdlKHt0YWc6IHRhZywgcmVzdWx0OiBtZXRhfSk7XG4gICAgICAgIH0pO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHBvc3RNZXNzYWdlKHt0YWc6IHRhZywgcmVzdWx0OiBtZXRhfSk7XG4gICAgfVxufVxuXG5CQklXb3JrZXJGZXRjaGVyLnByb3RvdHlwZS5sZWFwID0gZnVuY3Rpb24odGFnLCBjaHIsIHBvcywgZGlyKSB7XG4gICAgdGhpcy5iYmkuZ2V0VW56b29tZWRWaWV3KCkuZ2V0Rmlyc3RBZGphY2VudChjaHIsIHBvcywgZGlyLCBmdW5jdGlvbihyZXN1bHQsIGVycikge1xuICAgICAgICBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIHJlc3VsdDogcmVzdWx0LCBlcnJvcjogZXJyfSk7XG4gICAgfSk7XG59XG5cbkJCSVdvcmtlckZldGNoZXIucHJvdG90eXBlLnF1YW50TGVhcCA9IGZ1bmN0aW9uKHRhZywgY2hyLCBwb3MsIGRpciwgdGhyZXNob2xkLCB1bmRlcikge1xuICAgIHRoaXMuYmJpLnRocmVzaG9sZFNlYXJjaChjaHIsIHBvcywgZGlyLCB0aHJlc2hvbGQsIGZ1bmN0aW9uKHJlc3VsdCwgZXJyKSB7XG4gICAgICAgIHBvc3RNZXNzYWdlKHt0YWc6IHRhZywgcmVzdWx0OiByZXN1bHQsIGVycm9yOiBlcnJ9KTtcbiAgICB9KTtcbn1cblxuQkJJV29ya2VyRmV0Y2hlci5wcm90b3R5cGUuc2VhcmNoID0gZnVuY3Rpb24odGFnLCBxdWVyeSwgaW5kZXgpIHtcbiAgICB2YXIgaXMgPSB0aGlzLmV4dHJhSW5kaWNlc1swXTtcbiAgICBpcy5sb29rdXAocXVlcnksIGZ1bmN0aW9uKHJlc3VsdCwgZXJyKSB7XG4gICAgICAgIHBvc3RNZXNzYWdlKHt0YWc6IHRhZywgcmVzdWx0OiByZXN1bHQsIGVycm9yOiBlcnJ9KTtcbiAgICB9KTtcbn1cblxufSkuY2FsbCh0aGlzLHR5cGVvZiBzZWxmICE9PSBcInVuZGVmaW5lZFwiID8gc2VsZiA6IHR5cGVvZiB3aW5kb3cgIT09IFwidW5kZWZpbmVkXCIgPyB3aW5kb3cgOiB7fSkiLCIvKiAtKi0gbW9kZTogamF2YXNjcmlwdDsgYy1iYXNpYy1vZmZzZXQ6IDQ7IGluZGVudC10YWJzLW1vZGU6IG5pbCAtKi0gKi9cblxuLy8gXG4vLyBEYWxsaWFuY2UgR2Vub21lIEV4cGxvcmVyXG4vLyAoYykgVGhvbWFzIERvd24gMjAwNi0yMDExXG4vL1xuLy8gbGgzdXRpbHMuanM6IGNvbW1vbiBzdXBwb3J0IGZvciBsaDMncyBmaWxlIGZvcm1hdHNcbi8vXG5cbmlmICh0eXBlb2YocmVxdWlyZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgdmFyIGpzemxpYiA9IHJlcXVpcmUoJ2pzemxpYicpO1xuICAgIHZhciBqc3psaWJfaW5mbGF0ZV9idWZmZXIgPSBqc3psaWIuaW5mbGF0ZUJ1ZmZlcjtcbiAgICB2YXIgYXJyYXlDb3B5ID0ganN6bGliLmFycmF5Q29weTtcbn1cblxuZnVuY3Rpb24gVm9iKGIsIG8pIHtcbiAgICB0aGlzLmJsb2NrID0gYjtcbiAgICB0aGlzLm9mZnNldCA9IG87XG59XG5cblZvYi5wcm90b3R5cGUudG9TdHJpbmcgPSBmdW5jdGlvbigpIHtcbiAgICByZXR1cm4gJycgKyB0aGlzLmJsb2NrICsgJzonICsgdGhpcy5vZmZzZXQ7XG59XG5cbmZ1bmN0aW9uIHJlYWRWb2IoYmEsIG9mZnNldCkge1xuICAgIHZhciBibG9jayA9ICgoYmFbb2Zmc2V0KzZdICYgMHhmZikgKiAweDEwMDAwMDAwMCkgKyAoKGJhW29mZnNldCs1XSAmIDB4ZmYpICogMHgxMDAwMDAwKSArICgoYmFbb2Zmc2V0KzRdICYgMHhmZikgKiAweDEwMDAwKSArICgoYmFbb2Zmc2V0KzNdICYgMHhmZikgKiAweDEwMCkgKyAoKGJhW29mZnNldCsyXSAmIDB4ZmYpKTtcbiAgICB2YXIgYmludCA9IChiYVtvZmZzZXQrMV0gPDwgOCkgfCAoYmFbb2Zmc2V0XSk7XG4gICAgaWYgKGJsb2NrID09IDAgJiYgYmludCA9PSAwKSB7XG4gICAgICAgIHJldHVybiBudWxsOyAgLy8gU2hvdWxkIG9ubHkgaGFwcGVuIGluIHRoZSBsaW5lYXIgaW5kZXg/XG4gICAgfSBlbHNlIHtcbiAgICAgICAgcmV0dXJuIG5ldyBWb2IoYmxvY2ssIGJpbnQpO1xuICAgIH1cbn1cblxuZnVuY3Rpb24gdW5iZ3pmKGRhdGEsIGxpbSkge1xuICAgIGxpbSA9IE1hdGgubWluKGxpbSB8fCAxLCBkYXRhLmJ5dGVMZW5ndGggLSA1MCk7XG4gICAgdmFyIG9CbG9ja0xpc3QgPSBbXTtcbiAgICB2YXIgcHRyID0gWzBdO1xuICAgIHZhciB0b3RhbFNpemUgPSAwO1xuXG4gICAgd2hpbGUgKHB0clswXSA8IGxpbSkge1xuICAgICAgICB2YXIgYmEgPSBuZXcgVWludDhBcnJheShkYXRhLCBwdHJbMF0sIDEyKTsgLy8gRklYTUUgaXMgdGhpcyBlbm91Z2ggZm9yIGFsbCBjcmVkaWJsZSBCR1pGIGJsb2NrIGhlYWRlcnM/XG4gICAgICAgIHZhciB4bGVuID0gKGJhWzExXSA8PCA4KSB8IChiYVsxMF0pO1xuICAgICAgICAvLyBkbG9nKCd4bGVuWycgKyAocHRyWzBdKSArJ109JyArIHhsZW4pO1xuICAgICAgICB2YXIgdW5jID0ganN6bGliX2luZmxhdGVfYnVmZmVyKGRhdGEsIDEyICsgeGxlbiArIHB0clswXSwgTWF0aC5taW4oNjU1MzYsIGRhdGEuYnl0ZUxlbmd0aCAtIDEyIC0geGxlbiAtIHB0clswXSksIHB0cik7XG4gICAgICAgIHB0clswXSArPSA4O1xuICAgICAgICB0b3RhbFNpemUgKz0gdW5jLmJ5dGVMZW5ndGg7XG4gICAgICAgIG9CbG9ja0xpc3QucHVzaCh1bmMpO1xuICAgIH1cblxuICAgIGlmIChvQmxvY2tMaXN0Lmxlbmd0aCA9PSAxKSB7XG4gICAgICAgIHJldHVybiBvQmxvY2tMaXN0WzBdO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHZhciBvdXQgPSBuZXcgVWludDhBcnJheSh0b3RhbFNpemUpO1xuICAgICAgICB2YXIgY3Vyc29yID0gMDtcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBvQmxvY2tMaXN0Lmxlbmd0aDsgKytpKSB7XG4gICAgICAgICAgICB2YXIgYiA9IG5ldyBVaW50OEFycmF5KG9CbG9ja0xpc3RbaV0pO1xuICAgICAgICAgICAgYXJyYXlDb3B5KGIsIDAsIG91dCwgY3Vyc29yLCBiLmxlbmd0aCk7XG4gICAgICAgICAgICBjdXJzb3IgKz0gYi5sZW5ndGg7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIG91dC5idWZmZXI7XG4gICAgfVxufVxuXG5mdW5jdGlvbiBDaHVuayhtaW52LCBtYXh2KSB7XG4gICAgdGhpcy5taW52ID0gbWludjsgdGhpcy5tYXh2ID0gbWF4djtcbn1cblxuXG4vL1xuLy8gQmlubmluZyAodHJhbnNsaXRlcmF0ZWQgZnJvbSBTQU0xLjMgc3BlYylcbi8vXG5cbi8qIGNhbGN1bGF0ZSBiaW4gZ2l2ZW4gYW4gYWxpZ25tZW50IGNvdmVyaW5nIFtiZWcsZW5kKSAoemVyby1iYXNlZCwgaGFsZi1jbG9zZS1oYWxmLW9wZW4pICovXG5mdW5jdGlvbiByZWcyYmluKGJlZywgZW5kKVxue1xuICAgIC0tZW5kO1xuICAgIGlmIChiZWc+PjE0ID09IGVuZD4+MTQpIHJldHVybiAoKDE8PDE1KS0xKS83ICsgKGJlZz4+MTQpO1xuICAgIGlmIChiZWc+PjE3ID09IGVuZD4+MTcpIHJldHVybiAoKDE8PDEyKS0xKS83ICsgKGJlZz4+MTcpO1xuICAgIGlmIChiZWc+PjIwID09IGVuZD4+MjApIHJldHVybiAoKDE8PDkpLTEpLzcgKyAoYmVnPj4yMCk7XG4gICAgaWYgKGJlZz4+MjMgPT0gZW5kPj4yMykgcmV0dXJuICgoMTw8NiktMSkvNyArIChiZWc+PjIzKTtcbiAgICBpZiAoYmVnPj4yNiA9PSBlbmQ+PjI2KSByZXR1cm4gKCgxPDwzKS0xKS83ICsgKGJlZz4+MjYpO1xuICAgIHJldHVybiAwO1xufVxuXG4vKiBjYWxjdWxhdGUgdGhlIGxpc3Qgb2YgYmlucyB0aGF0IG1heSBvdmVybGFwIHdpdGggcmVnaW9uIFtiZWcsZW5kKSAoemVyby1iYXNlZCkgKi9cbnZhciBNQVhfQklOID0gKCgoMTw8MTgpLTEpLzcpO1xuZnVuY3Rpb24gcmVnMmJpbnMoYmVnLCBlbmQpIFxue1xuICAgIHZhciBpID0gMCwgaywgbGlzdCA9IFtdO1xuICAgIC0tZW5kO1xuICAgIGxpc3QucHVzaCgwKTtcbiAgICBmb3IgKGsgPSAxICsgKGJlZz4+MjYpOyBrIDw9IDEgKyAoZW5kPj4yNik7ICsraykgbGlzdC5wdXNoKGspO1xuICAgIGZvciAoayA9IDkgKyAoYmVnPj4yMyk7IGsgPD0gOSArIChlbmQ+PjIzKTsgKytrKSBsaXN0LnB1c2goayk7XG4gICAgZm9yIChrID0gNzMgKyAoYmVnPj4yMCk7IGsgPD0gNzMgKyAoZW5kPj4yMCk7ICsraykgbGlzdC5wdXNoKGspO1xuICAgIGZvciAoayA9IDU4NSArIChiZWc+PjE3KTsgayA8PSA1ODUgKyAoZW5kPj4xNyk7ICsraykgbGlzdC5wdXNoKGspO1xuICAgIGZvciAoayA9IDQ2ODEgKyAoYmVnPj4xNCk7IGsgPD0gNDY4MSArIChlbmQ+PjE0KTsgKytrKSBsaXN0LnB1c2goayk7XG4gICAgcmV0dXJuIGxpc3Q7XG59XG5cbmlmICh0eXBlb2YobW9kdWxlKSAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICBtb2R1bGUuZXhwb3J0cyA9IHtcbiAgICAgICAgdW5iZ3pmOiB1bmJnemYsXG4gICAgICAgIHJlYWRWb2I6IHJlYWRWb2IsXG4gICAgICAgIHJlZzJiaW46IHJlZzJiaW4sXG4gICAgICAgIHJlZzJiaW5zOiByZWcyYmlucyxcbiAgICAgICAgQ2h1bms6IENodW5rXG4gICAgfTtcbn0iLCIvKlxyXG4gKiBBIEphdmFTY3JpcHQgaW1wbGVtZW50YXRpb24gb2YgdGhlIFNlY3VyZSBIYXNoIEFsZ29yaXRobSwgU0hBLTEsIGFzIGRlZmluZWRcclxuICogaW4gRklQUyAxODAtMVxyXG4gKiBWZXJzaW9uIDIuMiBDb3B5cmlnaHQgUGF1bCBKb2huc3RvbiAyMDAwIC0gMjAwOS5cclxuICogT3RoZXIgY29udHJpYnV0b3JzOiBHcmVnIEhvbHQsIEFuZHJldyBLZXBlcnQsIFlkbmFyLCBMb3N0aW5ldFxyXG4gKiBEaXN0cmlidXRlZCB1bmRlciB0aGUgQlNEIExpY2Vuc2VcclxuICogU2VlIGh0dHA6Ly9wYWpob21lLm9yZy51ay9jcnlwdC9tZDUgZm9yIGRldGFpbHMuXHJcbiAqL1xyXG5cclxuIFwidXNlIHN0cmljdFwiO1xyXG5cclxuLypcclxuICogQ29uZmlndXJhYmxlIHZhcmlhYmxlcy4gWW91IG1heSBuZWVkIHRvIHR3ZWFrIHRoZXNlIHRvIGJlIGNvbXBhdGlibGUgd2l0aFxyXG4gKiB0aGUgc2VydmVyLXNpZGUsIGJ1dCB0aGUgZGVmYXVsdHMgd29yayBpbiBtb3N0IGNhc2VzLlxyXG4gKi9cclxudmFyIGhleGNhc2UgPSAwOyAgLyogaGV4IG91dHB1dCBmb3JtYXQuIDAgLSBsb3dlcmNhc2U7IDEgLSB1cHBlcmNhc2UgICAgICAgICovXHJcbnZhciBiNjRwYWQgID0gXCJcIjsgLyogYmFzZS02NCBwYWQgY2hhcmFjdGVyLiBcIj1cIiBmb3Igc3RyaWN0IFJGQyBjb21wbGlhbmNlICAgKi9cclxuXHJcbi8qXHJcbiAqIFRoZXNlIGFyZSB0aGUgZnVuY3Rpb25zIHlvdSdsbCB1c3VhbGx5IHdhbnQgdG8gY2FsbFxyXG4gKiBUaGV5IHRha2Ugc3RyaW5nIGFyZ3VtZW50cyBhbmQgcmV0dXJuIGVpdGhlciBoZXggb3IgYmFzZS02NCBlbmNvZGVkIHN0cmluZ3NcclxuICovXHJcbmZ1bmN0aW9uIGhleF9zaGExKHMpICAgIHsgcmV0dXJuIHJzdHIyaGV4KHJzdHJfc2hhMShzdHIycnN0cl91dGY4KHMpKSk7IH1cclxuZnVuY3Rpb24gYjY0X3NoYTEocykgICAgeyByZXR1cm4gcnN0cjJiNjQocnN0cl9zaGExKHN0cjJyc3RyX3V0ZjgocykpKTsgfVxyXG5mdW5jdGlvbiBhbnlfc2hhMShzLCBlKSB7IHJldHVybiByc3RyMmFueShyc3RyX3NoYTEoc3RyMnJzdHJfdXRmOChzKSksIGUpOyB9XHJcbmZ1bmN0aW9uIGhleF9obWFjX3NoYTEoaywgZClcclxuICB7IHJldHVybiByc3RyMmhleChyc3RyX2htYWNfc2hhMShzdHIycnN0cl91dGY4KGspLCBzdHIycnN0cl91dGY4KGQpKSk7IH1cclxuZnVuY3Rpb24gYjY0X2htYWNfc2hhMShrLCBkKVxyXG4gIHsgcmV0dXJuIHJzdHIyYjY0KHJzdHJfaG1hY19zaGExKHN0cjJyc3RyX3V0ZjgoayksIHN0cjJyc3RyX3V0ZjgoZCkpKTsgfVxyXG5mdW5jdGlvbiBhbnlfaG1hY19zaGExKGssIGQsIGUpXHJcbiAgeyByZXR1cm4gcnN0cjJhbnkocnN0cl9obWFjX3NoYTEoc3RyMnJzdHJfdXRmOChrKSwgc3RyMnJzdHJfdXRmOChkKSksIGUpOyB9XHJcblxyXG4vKlxyXG4gKiBQZXJmb3JtIGEgc2ltcGxlIHNlbGYtdGVzdCB0byBzZWUgaWYgdGhlIFZNIGlzIHdvcmtpbmdcclxuICovXHJcbmZ1bmN0aW9uIHNoYTFfdm1fdGVzdCgpXHJcbntcclxuICByZXR1cm4gaGV4X3NoYTEoXCJhYmNcIikudG9Mb3dlckNhc2UoKSA9PSBcImE5OTkzZTM2NDcwNjgxNmFiYTNlMjU3MTc4NTBjMjZjOWNkMGQ4OWRcIjtcclxufVxyXG5cclxuLypcclxuICogQ2FsY3VsYXRlIHRoZSBTSEExIG9mIGEgcmF3IHN0cmluZ1xyXG4gKi9cclxuZnVuY3Rpb24gcnN0cl9zaGExKHMpXHJcbntcclxuICByZXR1cm4gYmluYjJyc3RyKGJpbmJfc2hhMShyc3RyMmJpbmIocyksIHMubGVuZ3RoICogOCkpO1xyXG59XHJcblxyXG4vKlxyXG4gKiBDYWxjdWxhdGUgdGhlIEhNQUMtU0hBMSBvZiBhIGtleSBhbmQgc29tZSBkYXRhIChyYXcgc3RyaW5ncylcclxuICovXHJcbmZ1bmN0aW9uIHJzdHJfaG1hY19zaGExKGtleSwgZGF0YSlcclxue1xyXG4gIHZhciBia2V5ID0gcnN0cjJiaW5iKGtleSk7XHJcbiAgaWYoYmtleS5sZW5ndGggPiAxNikgYmtleSA9IGJpbmJfc2hhMShia2V5LCBrZXkubGVuZ3RoICogOCk7XHJcblxyXG4gIHZhciBpcGFkID0gQXJyYXkoMTYpLCBvcGFkID0gQXJyYXkoMTYpO1xyXG4gIGZvcih2YXIgaSA9IDA7IGkgPCAxNjsgaSsrKVxyXG4gIHtcclxuICAgIGlwYWRbaV0gPSBia2V5W2ldIF4gMHgzNjM2MzYzNjtcclxuICAgIG9wYWRbaV0gPSBia2V5W2ldIF4gMHg1QzVDNUM1QztcclxuICB9XHJcblxyXG4gIHZhciBoYXNoID0gYmluYl9zaGExKGlwYWQuY29uY2F0KHJzdHIyYmluYihkYXRhKSksIDUxMiArIGRhdGEubGVuZ3RoICogOCk7XHJcbiAgcmV0dXJuIGJpbmIycnN0cihiaW5iX3NoYTEob3BhZC5jb25jYXQoaGFzaCksIDUxMiArIDE2MCkpO1xyXG59XHJcblxyXG4vKlxyXG4gKiBDb252ZXJ0IGEgcmF3IHN0cmluZyB0byBhIGhleCBzdHJpbmdcclxuICovXHJcbmZ1bmN0aW9uIHJzdHIyaGV4KGlucHV0KVxyXG57XHJcbiAgLy8gdHJ5IHsgaGV4Y2FzZSB9IGNhdGNoKGUpIHsgaGV4Y2FzZT0wOyB9XHJcbiAgdmFyIGhleF90YWIgPSBoZXhjYXNlID8gXCIwMTIzNDU2Nzg5QUJDREVGXCIgOiBcIjAxMjM0NTY3ODlhYmNkZWZcIjtcclxuICB2YXIgb3V0cHV0ID0gXCJcIjtcclxuICB2YXIgeDtcclxuICBmb3IodmFyIGkgPSAwOyBpIDwgaW5wdXQubGVuZ3RoOyBpKyspXHJcbiAge1xyXG4gICAgeCA9IGlucHV0LmNoYXJDb2RlQXQoaSk7XHJcbiAgICBvdXRwdXQgKz0gaGV4X3RhYi5jaGFyQXQoKHggPj4+IDQpICYgMHgwRilcclxuICAgICAgICAgICArICBoZXhfdGFiLmNoYXJBdCggeCAgICAgICAgJiAweDBGKTtcclxuICB9XHJcbiAgcmV0dXJuIG91dHB1dDtcclxufVxyXG5cclxuLypcclxuICogQ29udmVydCBhIHJhdyBzdHJpbmcgdG8gYSBiYXNlLTY0IHN0cmluZ1xyXG4gKi9cclxuZnVuY3Rpb24gcnN0cjJiNjQoaW5wdXQpXHJcbntcclxuICAvLyB0cnkgeyBiNjRwYWQgfSBjYXRjaChlKSB7IGI2NHBhZD0nJzsgfVxyXG4gIHZhciB0YWIgPSBcIkFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg5Ky9cIjtcclxuICB2YXIgb3V0cHV0ID0gXCJcIjtcclxuICB2YXIgbGVuID0gaW5wdXQubGVuZ3RoO1xyXG4gIGZvcih2YXIgaSA9IDA7IGkgPCBsZW47IGkgKz0gMylcclxuICB7XHJcbiAgICB2YXIgdHJpcGxldCA9IChpbnB1dC5jaGFyQ29kZUF0KGkpIDw8IDE2KVxyXG4gICAgICAgICAgICAgICAgfCAoaSArIDEgPCBsZW4gPyBpbnB1dC5jaGFyQ29kZUF0KGkrMSkgPDwgOCA6IDApXHJcbiAgICAgICAgICAgICAgICB8IChpICsgMiA8IGxlbiA/IGlucHV0LmNoYXJDb2RlQXQoaSsyKSAgICAgIDogMCk7XHJcbiAgICBmb3IodmFyIGogPSAwOyBqIDwgNDsgaisrKVxyXG4gICAge1xyXG4gICAgICBpZihpICogOCArIGogKiA2ID4gaW5wdXQubGVuZ3RoICogOCkgb3V0cHV0ICs9IGI2NHBhZDtcclxuICAgICAgZWxzZSBvdXRwdXQgKz0gdGFiLmNoYXJBdCgodHJpcGxldCA+Pj4gNiooMy1qKSkgJiAweDNGKTtcclxuICAgIH1cclxuICB9XHJcbiAgcmV0dXJuIG91dHB1dDtcclxufVxyXG5cclxuLypcclxuICogQ29udmVydCBhIHJhdyBzdHJpbmcgdG8gYW4gYXJiaXRyYXJ5IHN0cmluZyBlbmNvZGluZ1xyXG4gKi9cclxuZnVuY3Rpb24gcnN0cjJhbnkoaW5wdXQsIGVuY29kaW5nKVxyXG57XHJcbiAgdmFyIGRpdmlzb3IgPSBlbmNvZGluZy5sZW5ndGg7XHJcbiAgdmFyIHJlbWFpbmRlcnMgPSBBcnJheSgpO1xyXG4gIHZhciBpLCBxLCB4LCBxdW90aWVudDtcclxuXHJcbiAgLyogQ29udmVydCB0byBhbiBhcnJheSBvZiAxNi1iaXQgYmlnLWVuZGlhbiB2YWx1ZXMsIGZvcm1pbmcgdGhlIGRpdmlkZW5kICovXHJcbiAgdmFyIGRpdmlkZW5kID0gQXJyYXkoTWF0aC5jZWlsKGlucHV0Lmxlbmd0aCAvIDIpKTtcclxuICBmb3IoaSA9IDA7IGkgPCBkaXZpZGVuZC5sZW5ndGg7IGkrKylcclxuICB7XHJcbiAgICBkaXZpZGVuZFtpXSA9IChpbnB1dC5jaGFyQ29kZUF0KGkgKiAyKSA8PCA4KSB8IGlucHV0LmNoYXJDb2RlQXQoaSAqIDIgKyAxKTtcclxuICB9XHJcblxyXG4gIC8qXHJcbiAgICogUmVwZWF0ZWRseSBwZXJmb3JtIGEgbG9uZyBkaXZpc2lvbi4gVGhlIGJpbmFyeSBhcnJheSBmb3JtcyB0aGUgZGl2aWRlbmQsXHJcbiAgICogdGhlIGxlbmd0aCBvZiB0aGUgZW5jb2RpbmcgaXMgdGhlIGRpdmlzb3IuIE9uY2UgY29tcHV0ZWQsIHRoZSBxdW90aWVudFxyXG4gICAqIGZvcm1zIHRoZSBkaXZpZGVuZCBmb3IgdGhlIG5leHQgc3RlcC4gV2Ugc3RvcCB3aGVuIHRoZSBkaXZpZGVuZCBpcyB6ZXJvLlxyXG4gICAqIEFsbCByZW1haW5kZXJzIGFyZSBzdG9yZWQgZm9yIGxhdGVyIHVzZS5cclxuICAgKi9cclxuICB3aGlsZShkaXZpZGVuZC5sZW5ndGggPiAwKVxyXG4gIHtcclxuICAgIHF1b3RpZW50ID0gQXJyYXkoKTtcclxuICAgIHggPSAwO1xyXG4gICAgZm9yKGkgPSAwOyBpIDwgZGl2aWRlbmQubGVuZ3RoOyBpKyspXHJcbiAgICB7XHJcbiAgICAgIHggPSAoeCA8PCAxNikgKyBkaXZpZGVuZFtpXTtcclxuICAgICAgcSA9IE1hdGguZmxvb3IoeCAvIGRpdmlzb3IpO1xyXG4gICAgICB4IC09IHEgKiBkaXZpc29yO1xyXG4gICAgICBpZihxdW90aWVudC5sZW5ndGggPiAwIHx8IHEgPiAwKVxyXG4gICAgICAgIHF1b3RpZW50W3F1b3RpZW50Lmxlbmd0aF0gPSBxO1xyXG4gICAgfVxyXG4gICAgcmVtYWluZGVyc1tyZW1haW5kZXJzLmxlbmd0aF0gPSB4O1xyXG4gICAgZGl2aWRlbmQgPSBxdW90aWVudDtcclxuICB9XHJcblxyXG4gIC8qIENvbnZlcnQgdGhlIHJlbWFpbmRlcnMgdG8gdGhlIG91dHB1dCBzdHJpbmcgKi9cclxuICB2YXIgb3V0cHV0ID0gXCJcIjtcclxuICBmb3IoaSA9IHJlbWFpbmRlcnMubGVuZ3RoIC0gMTsgaSA+PSAwOyBpLS0pXHJcbiAgICBvdXRwdXQgKz0gZW5jb2RpbmcuY2hhckF0KHJlbWFpbmRlcnNbaV0pO1xyXG5cclxuICAvKiBBcHBlbmQgbGVhZGluZyB6ZXJvIGVxdWl2YWxlbnRzICovXHJcbiAgdmFyIGZ1bGxfbGVuZ3RoID0gTWF0aC5jZWlsKGlucHV0Lmxlbmd0aCAqIDggL1xyXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAoTWF0aC5sb2coZW5jb2RpbmcubGVuZ3RoKSAvIE1hdGgubG9nKDIpKSlcclxuICBmb3IoaSA9IG91dHB1dC5sZW5ndGg7IGkgPCBmdWxsX2xlbmd0aDsgaSsrKVxyXG4gICAgb3V0cHV0ID0gZW5jb2RpbmdbMF0gKyBvdXRwdXQ7XHJcblxyXG4gIHJldHVybiBvdXRwdXQ7XHJcbn1cclxuXHJcbi8qXHJcbiAqIEVuY29kZSBhIHN0cmluZyBhcyB1dGYtOC5cclxuICogRm9yIGVmZmljaWVuY3ksIHRoaXMgYXNzdW1lcyB0aGUgaW5wdXQgaXMgdmFsaWQgdXRmLTE2LlxyXG4gKi9cclxuZnVuY3Rpb24gc3RyMnJzdHJfdXRmOChpbnB1dClcclxue1xyXG4gIHZhciBvdXRwdXQgPSBcIlwiO1xyXG4gIHZhciBpID0gLTE7XHJcbiAgdmFyIHgsIHk7XHJcblxyXG4gIHdoaWxlKCsraSA8IGlucHV0Lmxlbmd0aClcclxuICB7XHJcbiAgICAvKiBEZWNvZGUgdXRmLTE2IHN1cnJvZ2F0ZSBwYWlycyAqL1xyXG4gICAgeCA9IGlucHV0LmNoYXJDb2RlQXQoaSk7XHJcbiAgICB5ID0gaSArIDEgPCBpbnB1dC5sZW5ndGggPyBpbnB1dC5jaGFyQ29kZUF0KGkgKyAxKSA6IDA7XHJcbiAgICBpZigweEQ4MDAgPD0geCAmJiB4IDw9IDB4REJGRiAmJiAweERDMDAgPD0geSAmJiB5IDw9IDB4REZGRilcclxuICAgIHtcclxuICAgICAgeCA9IDB4MTAwMDAgKyAoKHggJiAweDAzRkYpIDw8IDEwKSArICh5ICYgMHgwM0ZGKTtcclxuICAgICAgaSsrO1xyXG4gICAgfVxyXG5cclxuICAgIC8qIEVuY29kZSBvdXRwdXQgYXMgdXRmLTggKi9cclxuICAgIGlmKHggPD0gMHg3RilcclxuICAgICAgb3V0cHV0ICs9IFN0cmluZy5mcm9tQ2hhckNvZGUoeCk7XHJcbiAgICBlbHNlIGlmKHggPD0gMHg3RkYpXHJcbiAgICAgIG91dHB1dCArPSBTdHJpbmcuZnJvbUNoYXJDb2RlKDB4QzAgfCAoKHggPj4+IDYgKSAmIDB4MUYpLFxyXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAweDgwIHwgKCB4ICAgICAgICAgJiAweDNGKSk7XHJcbiAgICBlbHNlIGlmKHggPD0gMHhGRkZGKVxyXG4gICAgICBvdXRwdXQgKz0gU3RyaW5nLmZyb21DaGFyQ29kZSgweEUwIHwgKCh4ID4+PiAxMikgJiAweDBGKSxcclxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgMHg4MCB8ICgoeCA+Pj4gNiApICYgMHgzRiksXHJcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIDB4ODAgfCAoIHggICAgICAgICAmIDB4M0YpKTtcclxuICAgIGVsc2UgaWYoeCA8PSAweDFGRkZGRilcclxuICAgICAgb3V0cHV0ICs9IFN0cmluZy5mcm9tQ2hhckNvZGUoMHhGMCB8ICgoeCA+Pj4gMTgpICYgMHgwNyksXHJcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIDB4ODAgfCAoKHggPj4+IDEyKSAmIDB4M0YpLFxyXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAweDgwIHwgKCh4ID4+PiA2ICkgJiAweDNGKSxcclxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgMHg4MCB8ICggeCAgICAgICAgICYgMHgzRikpO1xyXG4gIH1cclxuICByZXR1cm4gb3V0cHV0O1xyXG59XHJcblxyXG4vKlxyXG4gKiBFbmNvZGUgYSBzdHJpbmcgYXMgdXRmLTE2XHJcbiAqL1xyXG5mdW5jdGlvbiBzdHIycnN0cl91dGYxNmxlKGlucHV0KVxyXG57XHJcbiAgdmFyIG91dHB1dCA9IFwiXCI7XHJcbiAgZm9yKHZhciBpID0gMDsgaSA8IGlucHV0Lmxlbmd0aDsgaSsrKVxyXG4gICAgb3V0cHV0ICs9IFN0cmluZy5mcm9tQ2hhckNvZGUoIGlucHV0LmNoYXJDb2RlQXQoaSkgICAgICAgICYgMHhGRixcclxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIChpbnB1dC5jaGFyQ29kZUF0KGkpID4+PiA4KSAmIDB4RkYpO1xyXG4gIHJldHVybiBvdXRwdXQ7XHJcbn1cclxuXHJcbmZ1bmN0aW9uIHN0cjJyc3RyX3V0ZjE2YmUoaW5wdXQpXHJcbntcclxuICB2YXIgb3V0cHV0ID0gXCJcIjtcclxuICBmb3IodmFyIGkgPSAwOyBpIDwgaW5wdXQubGVuZ3RoOyBpKyspXHJcbiAgICBvdXRwdXQgKz0gU3RyaW5nLmZyb21DaGFyQ29kZSgoaW5wdXQuY2hhckNvZGVBdChpKSA+Pj4gOCkgJiAweEZGLFxyXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlucHV0LmNoYXJDb2RlQXQoaSkgICAgICAgICYgMHhGRik7XHJcbiAgcmV0dXJuIG91dHB1dDtcclxufVxyXG5cclxuLypcclxuICogQ29udmVydCBhIHJhdyBzdHJpbmcgdG8gYW4gYXJyYXkgb2YgYmlnLWVuZGlhbiB3b3Jkc1xyXG4gKiBDaGFyYWN0ZXJzID4yNTUgaGF2ZSB0aGVpciBoaWdoLWJ5dGUgc2lsZW50bHkgaWdub3JlZC5cclxuICovXHJcbmZ1bmN0aW9uIHJzdHIyYmluYihpbnB1dClcclxue1xyXG4gIHZhciBvdXRwdXQgPSBBcnJheShpbnB1dC5sZW5ndGggPj4gMik7XHJcbiAgZm9yKHZhciBpID0gMDsgaSA8IG91dHB1dC5sZW5ndGg7IGkrKylcclxuICAgIG91dHB1dFtpXSA9IDA7XHJcbiAgZm9yKHZhciBpID0gMDsgaSA8IGlucHV0Lmxlbmd0aCAqIDg7IGkgKz0gOClcclxuICAgIG91dHB1dFtpPj41XSB8PSAoaW5wdXQuY2hhckNvZGVBdChpIC8gOCkgJiAweEZGKSA8PCAoMjQgLSBpICUgMzIpO1xyXG4gIHJldHVybiBvdXRwdXQ7XHJcbn1cclxuXHJcbi8qXHJcbiAqIENvbnZlcnQgYW4gYXJyYXkgb2YgYmlnLWVuZGlhbiB3b3JkcyB0byBhIHN0cmluZ1xyXG4gKi9cclxuZnVuY3Rpb24gYmluYjJyc3RyKGlucHV0KVxyXG57XHJcbiAgdmFyIG91dHB1dCA9IFwiXCI7XHJcbiAgZm9yKHZhciBpID0gMDsgaSA8IGlucHV0Lmxlbmd0aCAqIDMyOyBpICs9IDgpXHJcbiAgICBvdXRwdXQgKz0gU3RyaW5nLmZyb21DaGFyQ29kZSgoaW5wdXRbaT4+NV0gPj4+ICgyNCAtIGkgJSAzMikpICYgMHhGRik7XHJcbiAgcmV0dXJuIG91dHB1dDtcclxufVxyXG5cclxuLypcclxuICogQ2FsY3VsYXRlIHRoZSBTSEEtMSBvZiBhbiBhcnJheSBvZiBiaWctZW5kaWFuIHdvcmRzLCBhbmQgYSBiaXQgbGVuZ3RoXHJcbiAqL1xyXG5mdW5jdGlvbiBiaW5iX3NoYTEoeCwgbGVuKVxyXG57XHJcbiAgLyogYXBwZW5kIHBhZGRpbmcgKi9cclxuICB4W2xlbiA+PiA1XSB8PSAweDgwIDw8ICgyNCAtIGxlbiAlIDMyKTtcclxuICB4WygobGVuICsgNjQgPj4gOSkgPDwgNCkgKyAxNV0gPSBsZW47XHJcblxyXG4gIHZhciB3ID0gQXJyYXkoODApO1xyXG4gIHZhciBhID0gIDE3MzI1ODQxOTM7XHJcbiAgdmFyIGIgPSAtMjcxNzMzODc5O1xyXG4gIHZhciBjID0gLTE3MzI1ODQxOTQ7XHJcbiAgdmFyIGQgPSAgMjcxNzMzODc4O1xyXG4gIHZhciBlID0gLTEwMDk1ODk3NzY7XHJcblxyXG4gIGZvcih2YXIgaSA9IDA7IGkgPCB4Lmxlbmd0aDsgaSArPSAxNilcclxuICB7XHJcbiAgICB2YXIgb2xkYSA9IGE7XHJcbiAgICB2YXIgb2xkYiA9IGI7XHJcbiAgICB2YXIgb2xkYyA9IGM7XHJcbiAgICB2YXIgb2xkZCA9IGQ7XHJcbiAgICB2YXIgb2xkZSA9IGU7XHJcblxyXG4gICAgZm9yKHZhciBqID0gMDsgaiA8IDgwOyBqKyspXHJcbiAgICB7XHJcbiAgICAgIGlmKGogPCAxNikgd1tqXSA9IHhbaSArIGpdO1xyXG4gICAgICBlbHNlIHdbal0gPSBiaXRfcm9sKHdbai0zXSBeIHdbai04XSBeIHdbai0xNF0gXiB3W2otMTZdLCAxKTtcclxuICAgICAgdmFyIHQgPSBzYWZlX2FkZChzYWZlX2FkZChiaXRfcm9sKGEsIDUpLCBzaGExX2Z0KGosIGIsIGMsIGQpKSxcclxuICAgICAgICAgICAgICAgICAgICAgICBzYWZlX2FkZChzYWZlX2FkZChlLCB3W2pdKSwgc2hhMV9rdChqKSkpO1xyXG4gICAgICBlID0gZDtcclxuICAgICAgZCA9IGM7XHJcbiAgICAgIGMgPSBiaXRfcm9sKGIsIDMwKTtcclxuICAgICAgYiA9IGE7XHJcbiAgICAgIGEgPSB0O1xyXG4gICAgfVxyXG5cclxuICAgIGEgPSBzYWZlX2FkZChhLCBvbGRhKTtcclxuICAgIGIgPSBzYWZlX2FkZChiLCBvbGRiKTtcclxuICAgIGMgPSBzYWZlX2FkZChjLCBvbGRjKTtcclxuICAgIGQgPSBzYWZlX2FkZChkLCBvbGRkKTtcclxuICAgIGUgPSBzYWZlX2FkZChlLCBvbGRlKTtcclxuICB9XHJcbiAgcmV0dXJuIEFycmF5KGEsIGIsIGMsIGQsIGUpO1xyXG5cclxufVxyXG5cclxuLypcclxuICogUGVyZm9ybSB0aGUgYXBwcm9wcmlhdGUgdHJpcGxldCBjb21iaW5hdGlvbiBmdW5jdGlvbiBmb3IgdGhlIGN1cnJlbnRcclxuICogaXRlcmF0aW9uXHJcbiAqL1xyXG5mdW5jdGlvbiBzaGExX2Z0KHQsIGIsIGMsIGQpXHJcbntcclxuICBpZih0IDwgMjApIHJldHVybiAoYiAmIGMpIHwgKCh+YikgJiBkKTtcclxuICBpZih0IDwgNDApIHJldHVybiBiIF4gYyBeIGQ7XHJcbiAgaWYodCA8IDYwKSByZXR1cm4gKGIgJiBjKSB8IChiICYgZCkgfCAoYyAmIGQpO1xyXG4gIHJldHVybiBiIF4gYyBeIGQ7XHJcbn1cclxuXHJcbi8qXHJcbiAqIERldGVybWluZSB0aGUgYXBwcm9wcmlhdGUgYWRkaXRpdmUgY29uc3RhbnQgZm9yIHRoZSBjdXJyZW50IGl0ZXJhdGlvblxyXG4gKi9cclxuZnVuY3Rpb24gc2hhMV9rdCh0KVxyXG57XHJcbiAgcmV0dXJuICh0IDwgMjApID8gIDE1MTg1MDAyNDkgOiAodCA8IDQwKSA/ICAxODU5Nzc1MzkzIDpcclxuICAgICAgICAgKHQgPCA2MCkgPyAtMTg5NDAwNzU4OCA6IC04OTk0OTc1MTQ7XHJcbn1cclxuXHJcbi8qXHJcbiAqIEFkZCBpbnRlZ2Vycywgd3JhcHBpbmcgYXQgMl4zMi4gVGhpcyB1c2VzIDE2LWJpdCBvcGVyYXRpb25zIGludGVybmFsbHlcclxuICogdG8gd29yayBhcm91bmQgYnVncyBpbiBzb21lIEpTIGludGVycHJldGVycy5cclxuICovXHJcbmZ1bmN0aW9uIHNhZmVfYWRkKHgsIHkpXHJcbntcclxuICB2YXIgbHN3ID0gKHggJiAweEZGRkYpICsgKHkgJiAweEZGRkYpO1xyXG4gIHZhciBtc3cgPSAoeCA+PiAxNikgKyAoeSA+PiAxNikgKyAobHN3ID4+IDE2KTtcclxuICByZXR1cm4gKG1zdyA8PCAxNikgfCAobHN3ICYgMHhGRkZGKTtcclxufVxyXG5cclxuLypcclxuICogQml0d2lzZSByb3RhdGUgYSAzMi1iaXQgbnVtYmVyIHRvIHRoZSBsZWZ0LlxyXG4gKi9cclxuZnVuY3Rpb24gYml0X3JvbChudW0sIGNudClcclxue1xyXG4gIHJldHVybiAobnVtIDw8IGNudCkgfCAobnVtID4+PiAoMzIgLSBjbnQpKTtcclxufVxyXG5cclxuaWYgKHR5cGVvZihtb2R1bGUpICE9PSAndW5kZWZpbmVkJykge1xyXG4gIG1vZHVsZS5leHBvcnRzID0ge1xyXG4gICAgYjY0X3NoYTE6IGI2NF9zaGExLFxyXG4gICAgaGV4X3NoYTE6IGhleF9zaGExXHJcbiAgfVxyXG59XHJcbiIsIi8qIC0qLSBtb2RlOiBqYXZhc2NyaXB0OyBjLWJhc2ljLW9mZnNldDogNDsgaW5kZW50LXRhYnMtbW9kZTogbmlsIC0qLSAqL1xuXG4vLyBcbi8vIERhbGxpYW5jZSBHZW5vbWUgRXhwbG9yZXJcbi8vIChjKSBUaG9tYXMgRG93biAyMDA2LTIwMTBcbi8vXG4vLyBzcGFucy5qczogSmF2YVNjcmlwdCBJbnRzZXQvTG9jYXRpb24gcG9ydC5cbi8vXG5cblwidXNlIHN0cmljdFwiO1xuXG5cbmZ1bmN0aW9uIFJhbmdlKG1pbiwgbWF4KVxue1xuICAgIGlmICh0eXBlb2YobWluKSAhPSAnbnVtYmVyJyB8fCB0eXBlb2YobWF4KSAhPSAnbnVtYmVyJylcbiAgICAgICAgdGhyb3cgJ0JhZCByYW5nZSAnICsgbWluICsgJywnICsgbWF4O1xuICAgIHRoaXMuX21pbiA9IG1pbjtcbiAgICB0aGlzLl9tYXggPSBtYXg7XG59XG5cblJhbmdlLnByb3RvdHlwZS5taW4gPSBmdW5jdGlvbigpIHtcbiAgICByZXR1cm4gdGhpcy5fbWluO1xufVxuXG5SYW5nZS5wcm90b3R5cGUubWF4ID0gZnVuY3Rpb24oKSB7XG4gICAgcmV0dXJuIHRoaXMuX21heDtcbn1cblxuUmFuZ2UucHJvdG90eXBlLmNvbnRhaW5zID0gZnVuY3Rpb24ocG9zKSB7XG4gICAgcmV0dXJuIHBvcyA+PSB0aGlzLl9taW4gJiYgcG9zIDw9IHRoaXMuX21heDtcbn1cblxuUmFuZ2UucHJvdG90eXBlLmlzQ29udGlndW91cyA9IGZ1bmN0aW9uKCkge1xuICAgIHJldHVybiB0cnVlO1xufVxuXG5SYW5nZS5wcm90b3R5cGUucmFuZ2VzID0gZnVuY3Rpb24oKSB7XG4gICAgcmV0dXJuIFt0aGlzXTtcbn1cblxuUmFuZ2UucHJvdG90eXBlLl9wdXNoUmFuZ2VzID0gZnVuY3Rpb24ocmFuZ2VzKSB7XG4gICAgcmFuZ2VzLnB1c2godGhpcyk7XG59XG5cblJhbmdlLnByb3RvdHlwZS50b1N0cmluZyA9IGZ1bmN0aW9uKCkge1xuICAgIHJldHVybiAnWycgKyB0aGlzLl9taW4gKyAnLScgKyB0aGlzLl9tYXggKyAnXSc7XG59XG5cbmZ1bmN0aW9uIF9Db21wb3VuZChyYW5nZXMpIHtcbiAgICB0aGlzLl9yYW5nZXMgPSByYW5nZXM7XG4gICAgLy8gYXNzZXJ0IHNvcnRlZD9cbn1cblxuX0NvbXBvdW5kLnByb3RvdHlwZS5taW4gPSBmdW5jdGlvbigpIHtcbiAgICByZXR1cm4gdGhpcy5fcmFuZ2VzWzBdLm1pbigpO1xufVxuXG5fQ29tcG91bmQucHJvdG90eXBlLm1heCA9IGZ1bmN0aW9uKCkge1xuICAgIHJldHVybiB0aGlzLl9yYW5nZXNbdGhpcy5fcmFuZ2VzLmxlbmd0aCAtIDFdLm1heCgpO1xufVxuXG5fQ29tcG91bmQucHJvdG90eXBlLmNvbnRhaW5zID0gZnVuY3Rpb24ocG9zKSB7XG4gICAgLy8gRklYTUUgaW1wbGVtZW50IGJzZWFyY2ggaWYgd2UgdXNlIHRoaXMgbXVjaC5cbiAgICBmb3IgKHZhciBzID0gMDsgcyA8IHRoaXMuX3Jhbmdlcy5sZW5ndGg7ICsrcykge1xuICAgICAgICBpZiAodGhpcy5fcmFuZ2VzW3NdLmNvbnRhaW5zKHBvcykpIHtcbiAgICAgICAgICAgIHJldHVybiB0cnVlO1xuICAgICAgICB9XG4gICAgfVxuICAgIHJldHVybiBmYWxzZTtcbn1cblxuX0NvbXBvdW5kLnByb3RvdHlwZS5pc0NvbnRpZ3VvdXMgPSBmdW5jdGlvbigpIHtcbiAgICByZXR1cm4gdGhpcy5fcmFuZ2VzLmxlbmd0aCA+IDE7XG59XG5cbl9Db21wb3VuZC5wcm90b3R5cGUucmFuZ2VzID0gZnVuY3Rpb24oKSB7XG4gICAgcmV0dXJuIHRoaXMuX3Jhbmdlcztcbn1cblxuX0NvbXBvdW5kLnByb3RvdHlwZS5fcHVzaFJhbmdlcyA9IGZ1bmN0aW9uKHJhbmdlcykge1xuICAgIGZvciAodmFyIHJpID0gMDsgcmkgPCB0aGlzLl9yYW5nZXMubGVuZ3RoOyArK3JpKVxuICAgICAgICByYW5nZXMucHVzaCh0aGlzLl9yYW5nZXNbcmldKTtcbn1cblxuX0NvbXBvdW5kLnByb3RvdHlwZS50b1N0cmluZyA9IGZ1bmN0aW9uKCkge1xuICAgIHZhciBzID0gJyc7XG4gICAgZm9yICh2YXIgciA9IDA7IHIgPCB0aGlzLl9yYW5nZXMubGVuZ3RoOyArK3IpIHtcbiAgICAgICAgaWYgKHI+MCkge1xuICAgICAgICAgICAgcyA9IHMgKyAnLCc7XG4gICAgICAgIH1cbiAgICAgICAgcyA9IHMgKyB0aGlzLl9yYW5nZXNbcl0udG9TdHJpbmcoKTtcbiAgICB9XG4gICAgcmV0dXJuIHM7XG59XG5cbmZ1bmN0aW9uIHVuaW9uKHMwLCBzMSkge1xuICAgIGlmICghIChzMCBpbnN0YW5jZW9mIEFycmF5KSkge1xuICAgICAgICBzMCA9IFtzMF07XG4gICAgICAgIGlmIChzMSlcbiAgICAgICAgICAgIHMwLnB1c2goczEpO1xuICAgIH1cblxuICAgIGlmIChzMC5sZW5ndGggPT0gMClcbiAgICAgICAgcmV0dXJuIG51bGw7XG4gICAgZWxzZSBpZiAoczAubGVuZ3RoID09IDEpXG4gICAgICAgIHJldHVybiBzMFswXTtcblxuICAgIHZhciByYW5nZXMgPSBbXTtcbiAgICBmb3IgKHZhciBzaSA9IDA7IHNpIDwgczAubGVuZ3RoOyArK3NpKVxuICAgICAgICBzMFtzaV0uX3B1c2hSYW5nZXMocmFuZ2VzKTtcbiAgICByYW5nZXMgPSByYW5nZXMuc29ydChfcmFuZ2VPcmRlcik7XG5cbiAgICB2YXIgb3JhbmdlcyA9IFtdO1xuICAgIHZhciBjdXJyZW50ID0gcmFuZ2VzWzBdO1xuICAgIGN1cnJlbnQgPSBuZXcgUmFuZ2UoY3VycmVudC5fbWluLCBjdXJyZW50Ll9tYXgpOyAgLy8gQ29weSBub3cgc28gd2UgZG9uJ3QgaGF2ZSB0byBsYXRlci5cblxuICAgIGZvciAodmFyIGkgPSAxOyBpIDwgcmFuZ2VzLmxlbmd0aDsgKytpKSB7XG4gICAgICAgIHZhciBueHQgPSByYW5nZXNbaV07XG4gICAgICAgIGlmIChueHQuX21pbiA+IChjdXJyZW50Ll9tYXggKyAxKSkge1xuICAgICAgICAgICAgb3Jhbmdlcy5wdXNoKGN1cnJlbnQpO1xuICAgICAgICAgICAgY3VycmVudCA9IG5ldyBSYW5nZShueHQuX21pbiwgbnh0Ll9tYXgpO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgaWYgKG54dC5fbWF4ID4gY3VycmVudC5fbWF4KSB7XG4gICAgICAgICAgICAgICAgY3VycmVudC5fbWF4ID0gbnh0Ll9tYXg7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9XG4gICAgb3Jhbmdlcy5wdXNoKGN1cnJlbnQpO1xuXG4gICAgaWYgKG9yYW5nZXMubGVuZ3RoID09IDEpIHtcbiAgICAgICAgcmV0dXJuIG9yYW5nZXNbMF07XG4gICAgfSBlbHNlIHtcbiAgICAgICAgcmV0dXJuIG5ldyBfQ29tcG91bmQob3Jhbmdlcyk7XG4gICAgfVxufVxuXG5mdW5jdGlvbiBpbnRlcnNlY3Rpb24oczAsIHMxKSB7XG4gICAgdmFyIHIwID0gczAucmFuZ2VzKCk7XG4gICAgdmFyIHIxID0gczEucmFuZ2VzKCk7XG4gICAgdmFyIGwwID0gcjAubGVuZ3RoLCBsMSA9IHIxLmxlbmd0aDtcbiAgICB2YXIgaTAgPSAwLCBpMSA9IDA7XG4gICAgdmFyIG9yID0gW107XG5cbiAgICB3aGlsZSAoaTAgPCBsMCAmJiBpMSA8IGwxKSB7XG4gICAgICAgIHZhciBzMCA9IHIwW2kwXSwgczEgPSByMVtpMV07XG4gICAgICAgIHZhciBsYXBNaW4gPSBNYXRoLm1heChzMC5taW4oKSwgczEubWluKCkpO1xuICAgICAgICB2YXIgbGFwTWF4ID0gTWF0aC5taW4oczAubWF4KCksIHMxLm1heCgpKTtcbiAgICAgICAgaWYgKGxhcE1heCA+PSBsYXBNaW4pIHtcbiAgICAgICAgICAgIG9yLnB1c2gobmV3IFJhbmdlKGxhcE1pbiwgbGFwTWF4KSk7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHMwLm1heCgpID4gczEubWF4KCkpIHtcbiAgICAgICAgICAgICsraTE7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICArK2kwO1xuICAgICAgICB9XG4gICAgfVxuICAgIFxuICAgIGlmIChvci5sZW5ndGggPT0gMCkge1xuICAgICAgICByZXR1cm4gbnVsbDsgLy8gRklYTUVcbiAgICB9IGVsc2UgaWYgKG9yLmxlbmd0aCA9PSAxKSB7XG4gICAgICAgIHJldHVybiBvclswXTtcbiAgICB9IGVsc2Uge1xuICAgICAgICByZXR1cm4gbmV3IF9Db21wb3VuZChvcik7XG4gICAgfVxufVxuXG5mdW5jdGlvbiBjb3ZlcmFnZShzKSB7XG4gICAgdmFyIHRvdCA9IDA7XG4gICAgdmFyIHJsID0gcy5yYW5nZXMoKTtcbiAgICBmb3IgKHZhciByaSA9IDA7IHJpIDwgcmwubGVuZ3RoOyArK3JpKSB7XG4gICAgICAgIHZhciByID0gcmxbcmldO1xuICAgICAgICB0b3QgKz0gKHIubWF4KCkgLSByLm1pbigpICsgMSk7XG4gICAgfVxuICAgIHJldHVybiB0b3Q7XG59XG5cblxuXG5mdW5jdGlvbiByYW5nZU9yZGVyKGEsIGIpXG57XG4gICAgaWYgKGEubWluKCkgPCBiLm1pbigpKSB7XG4gICAgICAgIHJldHVybiAtMTtcbiAgICB9IGVsc2UgaWYgKGEubWluKCkgPiBiLm1pbigpKSB7XG4gICAgICAgIHJldHVybiAxO1xuICAgIH0gZWxzZSBpZiAoYS5tYXgoKSA8IGIubWF4KCkpIHtcbiAgICAgICAgcmV0dXJuIC0xO1xuICAgIH0gZWxzZSBpZiAoYi5tYXgoKSA+IGEubWF4KCkpIHtcbiAgICAgICAgcmV0dXJuIDE7XG4gICAgfSBlbHNlIHtcbiAgICAgICAgcmV0dXJuIDA7XG4gICAgfVxufVxuXG5mdW5jdGlvbiBfcmFuZ2VPcmRlcihhLCBiKVxue1xuICAgIGlmIChhLl9taW4gPCBiLl9taW4pIHtcbiAgICAgICAgcmV0dXJuIC0xO1xuICAgIH0gZWxzZSBpZiAoYS5fbWluID4gYi5fbWluKSB7XG4gICAgICAgIHJldHVybiAxO1xuICAgIH0gZWxzZSBpZiAoYS5fbWF4IDwgYi5fbWF4KSB7XG4gICAgICAgIHJldHVybiAtMTtcbiAgICB9IGVsc2UgaWYgKGIuX21heCA+IGEuX21heCkge1xuICAgICAgICByZXR1cm4gMTtcbiAgICB9IGVsc2Uge1xuICAgICAgICByZXR1cm4gMDtcbiAgICB9XG59XG5cbmlmICh0eXBlb2YobW9kdWxlKSAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICBtb2R1bGUuZXhwb3J0cyA9IHtcbiAgICAgICAgUmFuZ2U6IFJhbmdlLFxuICAgICAgICB1bmlvbjogdW5pb24sXG4gICAgICAgIGludGVyc2VjdGlvbjogaW50ZXJzZWN0aW9uLFxuICAgICAgICBjb3ZlcmFnZTogY292ZXJhZ2UsXG4gICAgICAgIHJhbmdlT3ZlcjogcmFuZ2VPcmRlcixcbiAgICAgICAgX3JhbmdlT3JkZXI6IF9yYW5nZU9yZGVyXG4gICAgfVxufSIsIi8qIC0qLSBtb2RlOiBqYXZhc2NyaXB0OyBjLWJhc2ljLW9mZnNldDogNDsgaW5kZW50LXRhYnMtbW9kZTogbmlsIC0qLSAqL1xuXG4vLyBcbi8vIERhbGxpYW5jZSBHZW5vbWUgRXhwbG9yZXJcbi8vIChjKSBUaG9tYXMgRG93biAyMDA2LTIwMTBcbi8vXG4vLyB1dGlscy5qczogb2Rkcywgc29kcywgYW5kIGVuZHMuXG4vL1xuXG5cInVzZSBzdHJpY3RcIjtcblxuaWYgKHR5cGVvZihyZXF1aXJlKSAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICB2YXIgc2hhMSA9IHJlcXVpcmUoJy4vc2hhMScpO1xuICAgIHZhciBiNjRfc2hhMSA9IHNoYTEuYjY0X3NoYTE7XG59XG5cbnZhciBOVU1fUkVHRVhQID0gbmV3IFJlZ0V4cCgnWzAtOV0rJyk7XG5cbmZ1bmN0aW9uIHN0cmluZ1RvTnVtYmVyc0FycmF5KHN0cikge1xuICAgIHZhciBudW1zID0gbmV3IEFycmF5KCk7XG4gICAgdmFyIG07XG4gICAgd2hpbGUgKG0gPSBOVU1fUkVHRVhQLmV4ZWMoc3RyKSkge1xuICAgICAgICBudW1zLnB1c2gobVswXSk7XG4gICAgICAgIHN0cj1zdHIuc3Vic3RyaW5nKG0uaW5kZXggKyAobVswXS5sZW5ndGgpKTtcbiAgICB9XG4gICAgcmV0dXJuIG51bXM7XG59XG5cbnZhciBTVFJJQ1RfTlVNX1JFR0VYUCA9IG5ldyBSZWdFeHAoJ15bMC05XSskJyk7XG5cbmZ1bmN0aW9uIHN0cmluZ1RvSW50KHN0cikge1xuICAgIHN0ciA9IHN0ci5yZXBsYWNlKG5ldyBSZWdFeHAoJywnLCAnZycpLCAnJyk7XG4gICAgaWYgKCFTVFJJQ1RfTlVNX1JFR0VYUC50ZXN0KHN0cikpIHtcbiAgICAgICAgcmV0dXJuIG51bGw7XG4gICAgfVxuICAgIHJldHVybiBzdHJ8MDtcbn1cblxuZnVuY3Rpb24gcHVzaG5ldyhhLCB2KSB7XG4gICAgZm9yICh2YXIgaSA9IDA7IGkgPCBhLmxlbmd0aDsgKytpKSB7XG4gICAgICAgIGlmIChhW2ldID09IHYpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgIH1cbiAgICBhLnB1c2godik7XG59XG5cbmZ1bmN0aW9uIHB1c2hvKG9iaiwgaywgdikge1xuICAgIGlmIChvYmpba10pIHtcbiAgICAgICAgb2JqW2tdLnB1c2godik7XG4gICAgfSBlbHNlIHtcbiAgICAgICAgb2JqW2tdID0gW3ZdO1xuICAgIH1cbn1cblxuZnVuY3Rpb24gcHVzaG5ld28ob2JqLCBrLCB2KSB7XG4gICAgdmFyIGEgPSBvYmpba107XG4gICAgaWYgKGEpIHtcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBhLmxlbmd0aDsgKytpKSB7ICAgIC8vIGluZGV4T2YgcmVxdWlyZXMgSlMxNiA6LSguXG4gICAgICAgICAgICBpZiAoYVtpXSA9PSB2KSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGEucHVzaCh2KTtcbiAgICB9IGVsc2Uge1xuICAgICAgICBvYmpba10gPSBbdl07XG4gICAgfVxufVxuXG5cbmZ1bmN0aW9uIHBpY2soYSwgYiwgYywgZClcbntcbiAgICBpZiAoYSkge1xuICAgICAgICByZXR1cm4gYTtcbiAgICB9IGVsc2UgaWYgKGIpIHtcbiAgICAgICAgcmV0dXJuIGI7XG4gICAgfSBlbHNlIGlmIChjKSB7XG4gICAgICAgIHJldHVybiBjO1xuICAgIH0gZWxzZSBpZiAoZCkge1xuICAgICAgICByZXR1cm4gZDtcbiAgICB9XG59XG5cbmZ1bmN0aW9uIHB1c2huZXcobCwgbylcbntcbiAgICBmb3IgKHZhciBpID0gMDsgaSA8IGwubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgaWYgKGxbaV0gPT0gbykge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgfVxuICAgIGwucHVzaChvKTtcbn1cblxuXG5cbmZ1bmN0aW9uIGFycmF5SW5kZXhPZihhLCB4KSB7XG4gICAgaWYgKCFhKSB7XG4gICAgICAgIHJldHVybiAtMTtcbiAgICB9XG5cbiAgICBmb3IgKHZhciBpID0gMDsgaSA8IGEubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgaWYgKGFbaV0gPT09IHgpIHtcbiAgICAgICAgICAgIHJldHVybiBpO1xuICAgICAgICB9XG4gICAgfVxuICAgIHJldHVybiAtMTtcbn1cblxuZnVuY3Rpb24gYXJyYXlSZW1vdmUoYSwgeCkge1xuICAgIHZhciBpID0gYXJyYXlJbmRleE9mKGEsIHgpO1xuICAgIGlmIChpID49IDApIHtcbiAgICAgICAgYS5zcGxpY2UoaSwgMSk7XG4gICAgICAgIHJldHVybiB0cnVlO1xuICAgIH1cbiAgICByZXR1cm4gZmFsc2U7XG59XG5cbi8vXG4vLyBET00gdXRpbGl0aWVzXG4vL1xuXG5cbmZ1bmN0aW9uIG1ha2VFbGVtZW50KHRhZywgY2hpbGRyZW4sIGF0dHJpYnMsIHN0eWxlcylcbntcbiAgICB2YXIgZWxlID0gZG9jdW1lbnQuY3JlYXRlRWxlbWVudCh0YWcpO1xuICAgIGlmIChjaGlsZHJlbikge1xuICAgICAgICBpZiAoISAoY2hpbGRyZW4gaW5zdGFuY2VvZiBBcnJheSkpIHtcbiAgICAgICAgICAgIGNoaWxkcmVuID0gW2NoaWxkcmVuXTtcbiAgICAgICAgfVxuICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IGNoaWxkcmVuLmxlbmd0aDsgKytpKSB7XG4gICAgICAgICAgICB2YXIgYyA9IGNoaWxkcmVuW2ldO1xuICAgICAgICAgICAgaWYgKHR5cGVvZiBjID09ICdzdHJpbmcnKSB7XG4gICAgICAgICAgICAgICAgYyA9IGRvY3VtZW50LmNyZWF0ZVRleHROb2RlKGMpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgZWxlLmFwcGVuZENoaWxkKGMpO1xuICAgICAgICB9XG4gICAgfVxuICAgIFxuICAgIGlmIChhdHRyaWJzKSB7XG4gICAgICAgIGZvciAodmFyIGwgaW4gYXR0cmlicykge1xuICAgICAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgICAgICBlbGVbbF0gPSBhdHRyaWJzW2xdO1xuICAgICAgICAgICAgfSBjYXRjaCAoZSkge1xuICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCdlcnJvciBzZXR0aW5nICcgKyBsKTtcbiAgICAgICAgICAgICAgICB0aHJvdyhlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH1cbiAgICBpZiAoc3R5bGVzKSB7XG4gICAgICAgIGZvciAodmFyIGwgaW4gc3R5bGVzKSB7XG4gICAgICAgICAgICBlbGUuc3R5bGVbbF0gPSBzdHlsZXNbbF07XG4gICAgICAgIH1cbiAgICB9XG4gICAgcmV0dXJuIGVsZTtcbn1cblxuZnVuY3Rpb24gbWFrZUVsZW1lbnROUyhuYW1lc3BhY2UsIHRhZywgY2hpbGRyZW4sIGF0dHJpYnMpXG57XG4gICAgdmFyIGVsZSA9IGRvY3VtZW50LmNyZWF0ZUVsZW1lbnROUyhuYW1lc3BhY2UsIHRhZyk7XG4gICAgaWYgKGNoaWxkcmVuKSB7XG4gICAgICAgIGlmICghIChjaGlsZHJlbiBpbnN0YW5jZW9mIEFycmF5KSkge1xuICAgICAgICAgICAgY2hpbGRyZW4gPSBbY2hpbGRyZW5dO1xuICAgICAgICB9XG4gICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgY2hpbGRyZW4ubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgICAgIHZhciBjID0gY2hpbGRyZW5baV07XG4gICAgICAgICAgICBpZiAodHlwZW9mIGMgPT0gJ3N0cmluZycpIHtcbiAgICAgICAgICAgICAgICBjID0gZG9jdW1lbnQuY3JlYXRlVGV4dE5vZGUoYyk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBlbGUuYXBwZW5kQ2hpbGQoYyk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgXG4gICAgc2V0QXR0cnMoZWxlLCBhdHRyaWJzKTtcbiAgICByZXR1cm4gZWxlO1xufVxuXG52YXIgYXR0cl9uYW1lX2NhY2hlID0ge307XG5cbmZ1bmN0aW9uIHNldEF0dHIobm9kZSwga2V5LCB2YWx1ZSlcbntcbiAgICB2YXIgYXR0ciA9IGF0dHJfbmFtZV9jYWNoZVtrZXldO1xuICAgIGlmICghYXR0cikge1xuICAgICAgICB2YXIgX2F0dHIgPSAnJztcbiAgICAgICAgZm9yICh2YXIgYyA9IDA7IGMgPCBrZXkubGVuZ3RoOyArK2MpIHtcbiAgICAgICAgICAgIHZhciBjYyA9IGtleS5zdWJzdHJpbmcoYywgYysxKTtcbiAgICAgICAgICAgIHZhciBsY2MgPSBjYy50b0xvd2VyQ2FzZSgpO1xuICAgICAgICAgICAgaWYgKGxjYyAhPSBjYykge1xuICAgICAgICAgICAgICAgIF9hdHRyID0gX2F0dHIgKyAnLScgKyBsY2M7XG4gICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgIF9hdHRyID0gX2F0dHIgKyBjYztcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBhdHRyX25hbWVfY2FjaGVba2V5XSA9IF9hdHRyO1xuICAgICAgICBhdHRyID0gX2F0dHI7XG4gICAgfVxuICAgIG5vZGUuc2V0QXR0cmlidXRlKGF0dHIsIHZhbHVlKTtcbn1cblxuZnVuY3Rpb24gc2V0QXR0cnMobm9kZSwgYXR0cmlicylcbntcbiAgICBpZiAoYXR0cmlicykge1xuICAgICAgICBmb3IgKHZhciBsIGluIGF0dHJpYnMpIHtcbiAgICAgICAgICAgIHNldEF0dHIobm9kZSwgbCwgYXR0cmlic1tsXSk7XG4gICAgICAgIH1cbiAgICB9XG59XG5cblxuXG5mdW5jdGlvbiByZW1vdmVDaGlsZHJlbihub2RlKVxue1xuICAgIGlmICghbm9kZSB8fCAhbm9kZS5jaGlsZE5vZGVzKSB7XG4gICAgICAgIHJldHVybjtcbiAgICB9XG5cbiAgICB3aGlsZSAobm9kZS5jaGlsZE5vZGVzLmxlbmd0aCA+IDApIHtcbiAgICAgICAgbm9kZS5yZW1vdmVDaGlsZChub2RlLmZpcnN0Q2hpbGQpO1xuICAgIH1cbn1cblxuXG5cbi8vXG4vLyBXQVJOSU5HOiBub3QgZm9yIGdlbmVyYWwgdXNlIVxuLy9cblxuZnVuY3Rpb24gbWluaUpTT05pZnkobywgZXhjKSB7XG4gICAgaWYgKHR5cGVvZiBvID09PSAndW5kZWZpbmVkJykge1xuICAgICAgICByZXR1cm4gJ3VuZGVmaW5lZCc7XG4gICAgfSBlbHNlIGlmIChvID09IG51bGwpIHtcbiAgICAgICAgcmV0dXJuICdudWxsJztcbiAgICB9IGVsc2UgaWYgKHR5cGVvZiBvID09ICdzdHJpbmcnKSB7XG4gICAgICAgIHJldHVybiBcIidcIiArIG8gKyBcIidcIjtcbiAgICB9IGVsc2UgaWYgKHR5cGVvZiBvID09ICdudW1iZXInKSB7XG4gICAgICAgIHJldHVybiBcIlwiICsgbztcbiAgICB9IGVsc2UgaWYgKHR5cGVvZiBvID09ICdib29sZWFuJykge1xuICAgICAgICByZXR1cm4gXCJcIiArIG87XG4gICAgfSBlbHNlIGlmICh0eXBlb2YgbyA9PSAnb2JqZWN0Jykge1xuICAgICAgICBpZiAobyBpbnN0YW5jZW9mIEFycmF5KSB7XG4gICAgICAgICAgICB2YXIgcyA9IG51bGw7XG4gICAgICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IG8ubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgICAgICAgICBzID0gKHMgPT0gbnVsbCA/ICcnIDogKHMgKyAnLCAnKSkgKyBtaW5pSlNPTmlmeShvW2ldLCBleGMpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmV0dXJuICdbJyArIChzP3M6JycpICsgJ10nO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgZXhjID0gZXhjIHx8IHt9O1xuICAgICAgICAgICAgdmFyIHMgPSBudWxsO1xuICAgICAgICAgICAgZm9yICh2YXIgayBpbiBvKSB7XG4gICAgICAgICAgICAgICAgaWYgKGV4Y1trXSlcbiAgICAgICAgICAgICAgICAgICAgY29udGludWU7XG4gICAgICAgICAgICAgICAgaWYgKGsgIT0gdW5kZWZpbmVkICYmIHR5cGVvZihvW2tdKSAhPSAnZnVuY3Rpb24nKSB7XG4gICAgICAgICAgICAgICAgICAgIHMgPSAocyA9PSBudWxsID8gJycgOiAocyArICcsICcpKSArIGsgKyAnOiAnICsgbWluaUpTT05pZnkob1trXSwgZXhjKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZXR1cm4gJ3snICsgKHM/czonJykgKyAnfSc7XG4gICAgICAgIH1cbiAgICB9IGVsc2Uge1xuICAgICAgICByZXR1cm4gKHR5cGVvZiBvKTtcbiAgICB9XG59XG5cbmZ1bmN0aW9uIHNoYWxsb3dDb3B5KG8pIHtcbiAgICB2YXIgbiA9IHt9O1xuICAgIGZvciAodmFyIGsgaW4gbykge1xuICAgICAgICBuW2tdID0gb1trXTtcbiAgICB9XG4gICAgcmV0dXJuIG47XG59XG5cbmZ1bmN0aW9uIE9ic2VydmVkKHgpIHtcbiAgICB0aGlzLnZhbHVlID0geDtcbiAgICB0aGlzLmxpc3RlbmVycyA9IFtdO1xufVxuXG5PYnNlcnZlZC5wcm90b3R5cGUuYWRkTGlzdGVuZXIgPSBmdW5jdGlvbihmKSB7XG4gICAgdGhpcy5saXN0ZW5lcnMucHVzaChmKTtcbn1cblxuT2JzZXJ2ZWQucHJvdG90eXBlLmFkZExpc3RlbmVyQW5kRmlyZSA9IGZ1bmN0aW9uKGYpIHtcbiAgICB0aGlzLmxpc3RlbmVycy5wdXNoKGYpO1xuICAgIGYodGhpcy52YWx1ZSk7XG59XG5cbk9ic2VydmVkLnByb3RvdHlwZS5yZW1vdmVMaXN0ZW5lciA9IGZ1bmN0aW9uKGYpIHtcbiAgICBhcnJheVJlbW92ZSh0aGlzLmxpc3RlbmVycywgZik7XG59XG5cbk9ic2VydmVkLnByb3RvdHlwZS5nZXQgPSBmdW5jdGlvbigpIHtcbiAgICByZXR1cm4gdGhpcy52YWx1ZTtcbn1cblxuT2JzZXJ2ZWQucHJvdG90eXBlLnNldCA9IGZ1bmN0aW9uKHgpIHtcbiAgICB0aGlzLnZhbHVlID0geDtcbiAgICBmb3IgKHZhciBpID0gMDsgaSA8IHRoaXMubGlzdGVuZXJzLmxlbmd0aDsgKytpKSB7XG4gICAgICAgIHRoaXMubGlzdGVuZXJzW2ldKHgpO1xuICAgIH1cbn1cblxuZnVuY3Rpb24gQXdhaXRlZCgpIHtcbiAgICB0aGlzLnF1ZXVlID0gW107XG59XG5cbkF3YWl0ZWQucHJvdG90eXBlLnByb3ZpZGUgPSBmdW5jdGlvbih4KSB7XG4gICAgaWYgKHRoaXMucmVzICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgdGhyb3cgXCJSZXNvdXJjZSBoYXMgYWxyZWFkeSBiZWVuIHByb3ZpZGVkLlwiO1xuICAgIH1cblxuICAgIHRoaXMucmVzID0geDtcbiAgICBmb3IgKHZhciBpID0gMDsgaSA8IHRoaXMucXVldWUubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgdGhpcy5xdWV1ZVtpXSh4KTtcbiAgICB9XG4gICAgdGhpcy5xdWV1ZSA9IG51bGw7ICAgLy8gYXZvaWQgbGVha2luZyBjbG9zdXJlcy5cbn1cblxuQXdhaXRlZC5wcm90b3R5cGUuYXdhaXQgPSBmdW5jdGlvbihmKSB7XG4gICAgaWYgKHRoaXMucmVzICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgZih0aGlzLnJlcyk7XG4gICAgICAgIHJldHVybiB0aGlzLnJlcztcbiAgICB9IGVsc2Uge1xuICAgICAgICB0aGlzLnF1ZXVlLnB1c2goZik7XG4gICAgfVxufVxuXG52YXIgX19kYWxsaWFuY2Vfc2FsdFNlZWQgPSAwO1xuXG5mdW5jdGlvbiBzYWx0VVJMKHVybCkge1xuICAgIHJldHVybiB1cmwgKyAnP3NhbHQ9JyArIGI2NF9zaGExKCcnICsgRGF0ZS5ub3coKSArICcsJyArICgrK19fZGFsbGlhbmNlX3NhbHRTZWVkKSk7XG59XG5cbmZ1bmN0aW9uIHRleHRYSFIodXJsLCBjYWxsYmFjaywgb3B0cykge1xuICAgIGlmIChvcHRzLnNhbHQpIFxuICAgICAgICB1cmwgPSBzYWx0VVJMKHVybCk7XG5cbiAgICB2YXIgcmVxID0gbmV3IFhNTEh0dHBSZXF1ZXN0KCk7XG4gICAgcmVxLm9ucmVhZHlzdGF0ZWNoYW5nZSA9IGZ1bmN0aW9uKCkge1xuICAgIFx0aWYgKHJlcS5yZWFkeVN0YXRlID09IDQpIHtcbiAgICBcdCAgICBpZiAocmVxLnN0YXR1cyA+PSAzMDApIHtcbiAgICBcdFx0ICAgIGNhbGxiYWNrKG51bGwsICdFcnJvciBjb2RlICcgKyByZXEuc3RhdHVzKTtcbiAgICBcdCAgICB9IGVsc2Uge1xuICAgIFx0XHQgICAgY2FsbGJhY2socmVxLnJlc3BvbnNlVGV4dCk7XG4gICAgXHQgICAgfVxuICAgIFx0fVxuICAgIH07XG4gICAgXG4gICAgcmVxLm9wZW4oJ0dFVCcsIHVybCwgdHJ1ZSk7XG4gICAgcmVxLnJlc3BvbnNlVHlwZSA9ICd0ZXh0JztcblxuICAgIGlmIChvcHRzICYmIG9wdHMuY3JlZGVudGlhbHMpIHtcbiAgICAgICAgcmVxLndpdGhDcmVkZW50aWFscyA9IHRydWU7XG4gICAgfVxuICAgIHJlcS5zZW5kKCcnKTtcbn1cblxuZnVuY3Rpb24gcmVsYXRpdmVVUkwoYmFzZSwgcmVsKSB7XG4gICAgLy8gRklYTUUgcXVpdGUgbmFpdmUgLS0gZ29vZCBlbm91Z2ggZm9yIHRyYWNraHVicz9cblxuICAgIGlmIChyZWwuaW5kZXhPZignaHR0cDonKSA9PSAwIHx8IHJlbC5pbmRleE9mKCdodHRwczonKSA9PSAwKSB7XG4gICAgICAgIHJldHVybiByZWw7XG4gICAgfVxuXG4gICAgdmFyIGxpID0gYmFzZS5sYXN0SW5kZXhPZignLycpO1xuICAgIGlmIChsaSA+PSAwKSB7XG4gICAgICAgIHJldHVybiBiYXNlLnN1YnN0cigwLCBsaSArIDEpICsgcmVsO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHJldHVybiByZWw7XG4gICAgfVxufVxuXG4vL1xuLy8gTWlzc2luZyBBUElzXG4vLyBcblxuaWYgKCEoJ3RyaW0nIGluIFN0cmluZy5wcm90b3R5cGUpKSB7XG4gICAgU3RyaW5nLnByb3RvdHlwZS50cmltID0gZnVuY3Rpb24oKSB7XG4gICAgICAgIHJldHVybiB0aGlzLnJlcGxhY2UoL15cXHMrLywgJycpLnJlcGxhY2UoL1xccyskLywgJycpO1xuICAgIH07XG59XG5cbmlmICh0eXBlb2YobW9kdWxlKSAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICBtb2R1bGUuZXhwb3J0cyA9IHtcbiAgICAgICAgdGV4dFhIUjogdGV4dFhIUixcbiAgICAgICAgcmVsYXRpdmVVUkw6IHJlbGF0aXZlVVJMLFxuICAgICAgICBzaGFsbG93Q29weTogc2hhbGxvd0NvcHksXG4gICAgICAgIHB1c2hvOiBwdXNobyxcbiAgICAgICAgcHVzaG5ldzogcHVzaG5ldyxcbiAgICAgICAgcHVzaG5ld286IHB1c2huZXdvLFxuICAgICAgICBhcnJheUluZGV4T2Y6IGFycmF5SW5kZXhPZixcbiAgICAgICAgcGljazogcGljayxcblxuICAgICAgICBtYWtlRWxlbWVudDogbWFrZUVsZW1lbnQsXG4gICAgICAgIG1ha2VFbGVtZW50TlM6IG1ha2VFbGVtZW50TlMsXG4gICAgICAgIHJlbW92ZUNoaWxkcmVuOiByZW1vdmVDaGlsZHJlbixcblxuICAgICAgICBtaW5pSlNPTmlmeTogbWluaUpTT05pZnksXG5cbiAgICAgICAgT2JzZXJ2ZWQ6IE9ic2VydmVkLFxuICAgICAgICBBd2FpdGVkOiBBd2FpdGVkXG4gICAgfVxufVxuIiwiLyogLSotIG1vZGU6IGphdmFzY3JpcHQ7IGMtYmFzaWMtb2Zmc2V0OiA0OyBpbmRlbnQtdGFicy1tb2RlOiBuaWwgLSotICovXG5cbi8vIFxuLy8gSmF2YXNjcmlwdCBaTGliXG4vLyBCeSBUaG9tYXMgRG93biAyMDEwLTIwMTFcbi8vXG4vLyBCYXNlZCB2ZXJ5IGhlYXZpbHkgb24gcG9ydGlvbnMgb2YganpsaWIgKGJ5IHltbmtAamNyYWZ0LmNvbSksIHdobyBpblxuLy8gdHVybiBjcmVkaXRzIEplYW4tbG91cCBHYWlsbHkgYW5kIE1hcmsgQWRsZXIgZm9yIHRoZSBvcmlnaW5hbCB6bGliIGNvZGUuXG4vL1xuLy8gaW5mbGF0ZS5qczogWkxpYiBpbmZsYXRlIGNvZGVcbi8vXG5cbi8vXG4vLyBTaGFyZWQgY29uc3RhbnRzXG4vL1xuXG52YXIgTUFYX1dCSVRTPTE1OyAvLyAzMksgTFo3NyB3aW5kb3dcbnZhciBERUZfV0JJVFM9TUFYX1dCSVRTO1xudmFyIE1BWF9NRU1fTEVWRUw9OTtcbnZhciBNQU5ZPTE0NDA7XG52YXIgQk1BWCA9IDE1O1xuXG4vLyBwcmVzZXQgZGljdGlvbmFyeSBmbGFnIGluIHpsaWIgaGVhZGVyXG52YXIgUFJFU0VUX0RJQ1Q9MHgyMDtcblxudmFyIFpfTk9fRkxVU0g9MDtcbnZhciBaX1BBUlRJQUxfRkxVU0g9MTtcbnZhciBaX1NZTkNfRkxVU0g9MjtcbnZhciBaX0ZVTExfRkxVU0g9MztcbnZhciBaX0ZJTklTSD00O1xuXG52YXIgWl9ERUZMQVRFRD04O1xuXG52YXIgWl9PSz0wO1xudmFyIFpfU1RSRUFNX0VORD0xO1xudmFyIFpfTkVFRF9ESUNUPTI7XG52YXIgWl9FUlJOTz0tMTtcbnZhciBaX1NUUkVBTV9FUlJPUj0tMjtcbnZhciBaX0RBVEFfRVJST1I9LTM7XG52YXIgWl9NRU1fRVJST1I9LTQ7XG52YXIgWl9CVUZfRVJST1I9LTU7XG52YXIgWl9WRVJTSU9OX0VSUk9SPS02O1xuXG52YXIgTUVUSE9EPTA7ICAgLy8gd2FpdGluZyBmb3IgbWV0aG9kIGJ5dGVcbnZhciBGTEFHPTE7ICAgICAvLyB3YWl0aW5nIGZvciBmbGFnIGJ5dGVcbnZhciBESUNUND0yOyAgICAvLyBmb3VyIGRpY3Rpb25hcnkgY2hlY2sgYnl0ZXMgdG8gZ29cbnZhciBESUNUMz0zOyAgICAvLyB0aHJlZSBkaWN0aW9uYXJ5IGNoZWNrIGJ5dGVzIHRvIGdvXG52YXIgRElDVDI9NDsgICAgLy8gdHdvIGRpY3Rpb25hcnkgY2hlY2sgYnl0ZXMgdG8gZ29cbnZhciBESUNUMT01OyAgICAvLyBvbmUgZGljdGlvbmFyeSBjaGVjayBieXRlIHRvIGdvXG52YXIgRElDVDA9NjsgICAgLy8gd2FpdGluZyBmb3IgaW5mbGF0ZVNldERpY3Rpb25hcnlcbnZhciBCTE9DS1M9NzsgICAvLyBkZWNvbXByZXNzaW5nIGJsb2Nrc1xudmFyIENIRUNLND04OyAgIC8vIGZvdXIgY2hlY2sgYnl0ZXMgdG8gZ29cbnZhciBDSEVDSzM9OTsgICAvLyB0aHJlZSBjaGVjayBieXRlcyB0byBnb1xudmFyIENIRUNLMj0xMDsgIC8vIHR3byBjaGVjayBieXRlcyB0byBnb1xudmFyIENIRUNLMT0xMTsgIC8vIG9uZSBjaGVjayBieXRlIHRvIGdvXG52YXIgRE9ORT0xMjsgICAgLy8gZmluaXNoZWQgY2hlY2ssIGRvbmVcbnZhciBCQUQ9MTM7ICAgICAvLyBnb3QgYW4gZXJyb3ItLXN0YXkgaGVyZVxuXG52YXIgaW5mbGF0ZV9tYXNrID0gWzB4MDAwMDAwMDAsIDB4MDAwMDAwMDEsIDB4MDAwMDAwMDMsIDB4MDAwMDAwMDcsIDB4MDAwMDAwMGYsIDB4MDAwMDAwMWYsIDB4MDAwMDAwM2YsIDB4MDAwMDAwN2YsIDB4MDAwMDAwZmYsIDB4MDAwMDAxZmYsIDB4MDAwMDAzZmYsIDB4MDAwMDA3ZmYsIDB4MDAwMDBmZmYsIDB4MDAwMDFmZmYsIDB4MDAwMDNmZmYsIDB4MDAwMDdmZmYsIDB4MDAwMGZmZmZdO1xuXG52YXIgSUJfVFlQRT0wOyAgLy8gZ2V0IHR5cGUgYml0cyAoMywgaW5jbHVkaW5nIGVuZCBiaXQpXG52YXIgSUJfTEVOUz0xOyAgLy8gZ2V0IGxlbmd0aHMgZm9yIHN0b3JlZFxudmFyIElCX1NUT1JFRD0yOy8vIHByb2Nlc3Npbmcgc3RvcmVkIGJsb2NrXG52YXIgSUJfVEFCTEU9MzsgLy8gZ2V0IHRhYmxlIGxlbmd0aHNcbnZhciBJQl9CVFJFRT00OyAvLyBnZXQgYml0IGxlbmd0aHMgdHJlZSBmb3IgYSBkeW5hbWljIGJsb2NrXG52YXIgSUJfRFRSRUU9NTsgLy8gZ2V0IGxlbmd0aCwgZGlzdGFuY2UgdHJlZXMgZm9yIGEgZHluYW1pYyBibG9ja1xudmFyIElCX0NPREVTPTY7IC8vIHByb2Nlc3NpbmcgZml4ZWQgb3IgZHluYW1pYyBibG9ja1xudmFyIElCX0RSWT03OyAgIC8vIG91dHB1dCByZW1haW5pbmcgd2luZG93IGJ5dGVzXG52YXIgSUJfRE9ORT04OyAgLy8gZmluaXNoZWQgbGFzdCBibG9jaywgZG9uZVxudmFyIElCX0JBRD05OyAgIC8vIG90IGEgZGF0YSBlcnJvci0tc3R1Y2sgaGVyZVxuXG52YXIgZml4ZWRfYmwgPSA5O1xudmFyIGZpeGVkX2JkID0gNTtcblxudmFyIGZpeGVkX3RsID0gW1xuICAgIDk2LDcsMjU2LCAwLDgsODAsIDAsOCwxNiwgODQsOCwxMTUsXG4gICAgODIsNywzMSwgMCw4LDExMiwgMCw4LDQ4LCAwLDksMTkyLFxuICAgIDgwLDcsMTAsIDAsOCw5NiwgMCw4LDMyLCAwLDksMTYwLFxuICAgIDAsOCwwLCAwLDgsMTI4LCAwLDgsNjQsIDAsOSwyMjQsXG4gICAgODAsNyw2LCAwLDgsODgsIDAsOCwyNCwgMCw5LDE0NCxcbiAgICA4Myw3LDU5LCAwLDgsMTIwLCAwLDgsNTYsIDAsOSwyMDgsXG4gICAgODEsNywxNywgMCw4LDEwNCwgMCw4LDQwLCAwLDksMTc2LFxuICAgIDAsOCw4LCAwLDgsMTM2LCAwLDgsNzIsIDAsOSwyNDAsXG4gICAgODAsNyw0LCAwLDgsODQsIDAsOCwyMCwgODUsOCwyMjcsXG4gICAgODMsNyw0MywgMCw4LDExNiwgMCw4LDUyLCAwLDksMjAwLFxuICAgIDgxLDcsMTMsIDAsOCwxMDAsIDAsOCwzNiwgMCw5LDE2OCxcbiAgICAwLDgsNCwgMCw4LDEzMiwgMCw4LDY4LCAwLDksMjMyLFxuICAgIDgwLDcsOCwgMCw4LDkyLCAwLDgsMjgsIDAsOSwxNTIsXG4gICAgODQsNyw4MywgMCw4LDEyNCwgMCw4LDYwLCAwLDksMjE2LFxuICAgIDgyLDcsMjMsIDAsOCwxMDgsIDAsOCw0NCwgMCw5LDE4NCxcbiAgICAwLDgsMTIsIDAsOCwxNDAsIDAsOCw3NiwgMCw5LDI0OCxcbiAgICA4MCw3LDMsIDAsOCw4MiwgMCw4LDE4LCA4NSw4LDE2MyxcbiAgICA4Myw3LDM1LCAwLDgsMTE0LCAwLDgsNTAsIDAsOSwxOTYsXG4gICAgODEsNywxMSwgMCw4LDk4LCAwLDgsMzQsIDAsOSwxNjQsXG4gICAgMCw4LDIsIDAsOCwxMzAsIDAsOCw2NiwgMCw5LDIyOCxcbiAgICA4MCw3LDcsIDAsOCw5MCwgMCw4LDI2LCAwLDksMTQ4LFxuICAgIDg0LDcsNjcsIDAsOCwxMjIsIDAsOCw1OCwgMCw5LDIxMixcbiAgICA4Miw3LDE5LCAwLDgsMTA2LCAwLDgsNDIsIDAsOSwxODAsXG4gICAgMCw4LDEwLCAwLDgsMTM4LCAwLDgsNzQsIDAsOSwyNDQsXG4gICAgODAsNyw1LCAwLDgsODYsIDAsOCwyMiwgMTkyLDgsMCxcbiAgICA4Myw3LDUxLCAwLDgsMTE4LCAwLDgsNTQsIDAsOSwyMDQsXG4gICAgODEsNywxNSwgMCw4LDEwMiwgMCw4LDM4LCAwLDksMTcyLFxuICAgIDAsOCw2LCAwLDgsMTM0LCAwLDgsNzAsIDAsOSwyMzYsXG4gICAgODAsNyw5LCAwLDgsOTQsIDAsOCwzMCwgMCw5LDE1NixcbiAgICA4NCw3LDk5LCAwLDgsMTI2LCAwLDgsNjIsIDAsOSwyMjAsXG4gICAgODIsNywyNywgMCw4LDExMCwgMCw4LDQ2LCAwLDksMTg4LFxuICAgIDAsOCwxNCwgMCw4LDE0MiwgMCw4LDc4LCAwLDksMjUyLFxuICAgIDk2LDcsMjU2LCAwLDgsODEsIDAsOCwxNywgODUsOCwxMzEsXG4gICAgODIsNywzMSwgMCw4LDExMywgMCw4LDQ5LCAwLDksMTk0LFxuICAgIDgwLDcsMTAsIDAsOCw5NywgMCw4LDMzLCAwLDksMTYyLFxuICAgIDAsOCwxLCAwLDgsMTI5LCAwLDgsNjUsIDAsOSwyMjYsXG4gICAgODAsNyw2LCAwLDgsODksIDAsOCwyNSwgMCw5LDE0NixcbiAgICA4Myw3LDU5LCAwLDgsMTIxLCAwLDgsNTcsIDAsOSwyMTAsXG4gICAgODEsNywxNywgMCw4LDEwNSwgMCw4LDQxLCAwLDksMTc4LFxuICAgIDAsOCw5LCAwLDgsMTM3LCAwLDgsNzMsIDAsOSwyNDIsXG4gICAgODAsNyw0LCAwLDgsODUsIDAsOCwyMSwgODAsOCwyNTgsXG4gICAgODMsNyw0MywgMCw4LDExNywgMCw4LDUzLCAwLDksMjAyLFxuICAgIDgxLDcsMTMsIDAsOCwxMDEsIDAsOCwzNywgMCw5LDE3MCxcbiAgICAwLDgsNSwgMCw4LDEzMywgMCw4LDY5LCAwLDksMjM0LFxuICAgIDgwLDcsOCwgMCw4LDkzLCAwLDgsMjksIDAsOSwxNTQsXG4gICAgODQsNyw4MywgMCw4LDEyNSwgMCw4LDYxLCAwLDksMjE4LFxuICAgIDgyLDcsMjMsIDAsOCwxMDksIDAsOCw0NSwgMCw5LDE4NixcbiAgICAwLDgsMTMsIDAsOCwxNDEsIDAsOCw3NywgMCw5LDI1MCxcbiAgICA4MCw3LDMsIDAsOCw4MywgMCw4LDE5LCA4NSw4LDE5NSxcbiAgICA4Myw3LDM1LCAwLDgsMTE1LCAwLDgsNTEsIDAsOSwxOTgsXG4gICAgODEsNywxMSwgMCw4LDk5LCAwLDgsMzUsIDAsOSwxNjYsXG4gICAgMCw4LDMsIDAsOCwxMzEsIDAsOCw2NywgMCw5LDIzMCxcbiAgICA4MCw3LDcsIDAsOCw5MSwgMCw4LDI3LCAwLDksMTUwLFxuICAgIDg0LDcsNjcsIDAsOCwxMjMsIDAsOCw1OSwgMCw5LDIxNCxcbiAgICA4Miw3LDE5LCAwLDgsMTA3LCAwLDgsNDMsIDAsOSwxODIsXG4gICAgMCw4LDExLCAwLDgsMTM5LCAwLDgsNzUsIDAsOSwyNDYsXG4gICAgODAsNyw1LCAwLDgsODcsIDAsOCwyMywgMTkyLDgsMCxcbiAgICA4Myw3LDUxLCAwLDgsMTE5LCAwLDgsNTUsIDAsOSwyMDYsXG4gICAgODEsNywxNSwgMCw4LDEwMywgMCw4LDM5LCAwLDksMTc0LFxuICAgIDAsOCw3LCAwLDgsMTM1LCAwLDgsNzEsIDAsOSwyMzgsXG4gICAgODAsNyw5LCAwLDgsOTUsIDAsOCwzMSwgMCw5LDE1OCxcbiAgICA4NCw3LDk5LCAwLDgsMTI3LCAwLDgsNjMsIDAsOSwyMjIsXG4gICAgODIsNywyNywgMCw4LDExMSwgMCw4LDQ3LCAwLDksMTkwLFxuICAgIDAsOCwxNSwgMCw4LDE0MywgMCw4LDc5LCAwLDksMjU0LFxuICAgIDk2LDcsMjU2LCAwLDgsODAsIDAsOCwxNiwgODQsOCwxMTUsXG4gICAgODIsNywzMSwgMCw4LDExMiwgMCw4LDQ4LCAwLDksMTkzLFxuXG4gICAgODAsNywxMCwgMCw4LDk2LCAwLDgsMzIsIDAsOSwxNjEsXG4gICAgMCw4LDAsIDAsOCwxMjgsIDAsOCw2NCwgMCw5LDIyNSxcbiAgICA4MCw3LDYsIDAsOCw4OCwgMCw4LDI0LCAwLDksMTQ1LFxuICAgIDgzLDcsNTksIDAsOCwxMjAsIDAsOCw1NiwgMCw5LDIwOSxcbiAgICA4MSw3LDE3LCAwLDgsMTA0LCAwLDgsNDAsIDAsOSwxNzcsXG4gICAgMCw4LDgsIDAsOCwxMzYsIDAsOCw3MiwgMCw5LDI0MSxcbiAgICA4MCw3LDQsIDAsOCw4NCwgMCw4LDIwLCA4NSw4LDIyNyxcbiAgICA4Myw3LDQzLCAwLDgsMTE2LCAwLDgsNTIsIDAsOSwyMDEsXG4gICAgODEsNywxMywgMCw4LDEwMCwgMCw4LDM2LCAwLDksMTY5LFxuICAgIDAsOCw0LCAwLDgsMTMyLCAwLDgsNjgsIDAsOSwyMzMsXG4gICAgODAsNyw4LCAwLDgsOTIsIDAsOCwyOCwgMCw5LDE1MyxcbiAgICA4NCw3LDgzLCAwLDgsMTI0LCAwLDgsNjAsIDAsOSwyMTcsXG4gICAgODIsNywyMywgMCw4LDEwOCwgMCw4LDQ0LCAwLDksMTg1LFxuICAgIDAsOCwxMiwgMCw4LDE0MCwgMCw4LDc2LCAwLDksMjQ5LFxuICAgIDgwLDcsMywgMCw4LDgyLCAwLDgsMTgsIDg1LDgsMTYzLFxuICAgIDgzLDcsMzUsIDAsOCwxMTQsIDAsOCw1MCwgMCw5LDE5NyxcbiAgICA4MSw3LDExLCAwLDgsOTgsIDAsOCwzNCwgMCw5LDE2NSxcbiAgICAwLDgsMiwgMCw4LDEzMCwgMCw4LDY2LCAwLDksMjI5LFxuICAgIDgwLDcsNywgMCw4LDkwLCAwLDgsMjYsIDAsOSwxNDksXG4gICAgODQsNyw2NywgMCw4LDEyMiwgMCw4LDU4LCAwLDksMjEzLFxuICAgIDgyLDcsMTksIDAsOCwxMDYsIDAsOCw0MiwgMCw5LDE4MSxcbiAgICAwLDgsMTAsIDAsOCwxMzgsIDAsOCw3NCwgMCw5LDI0NSxcbiAgICA4MCw3LDUsIDAsOCw4NiwgMCw4LDIyLCAxOTIsOCwwLFxuICAgIDgzLDcsNTEsIDAsOCwxMTgsIDAsOCw1NCwgMCw5LDIwNSxcbiAgICA4MSw3LDE1LCAwLDgsMTAyLCAwLDgsMzgsIDAsOSwxNzMsXG4gICAgMCw4LDYsIDAsOCwxMzQsIDAsOCw3MCwgMCw5LDIzNyxcbiAgICA4MCw3LDksIDAsOCw5NCwgMCw4LDMwLCAwLDksMTU3LFxuICAgIDg0LDcsOTksIDAsOCwxMjYsIDAsOCw2MiwgMCw5LDIyMSxcbiAgICA4Miw3LDI3LCAwLDgsMTEwLCAwLDgsNDYsIDAsOSwxODksXG4gICAgMCw4LDE0LCAwLDgsMTQyLCAwLDgsNzgsIDAsOSwyNTMsXG4gICAgOTYsNywyNTYsIDAsOCw4MSwgMCw4LDE3LCA4NSw4LDEzMSxcbiAgICA4Miw3LDMxLCAwLDgsMTEzLCAwLDgsNDksIDAsOSwxOTUsXG4gICAgODAsNywxMCwgMCw4LDk3LCAwLDgsMzMsIDAsOSwxNjMsXG4gICAgMCw4LDEsIDAsOCwxMjksIDAsOCw2NSwgMCw5LDIyNyxcbiAgICA4MCw3LDYsIDAsOCw4OSwgMCw4LDI1LCAwLDksMTQ3LFxuICAgIDgzLDcsNTksIDAsOCwxMjEsIDAsOCw1NywgMCw5LDIxMSxcbiAgICA4MSw3LDE3LCAwLDgsMTA1LCAwLDgsNDEsIDAsOSwxNzksXG4gICAgMCw4LDksIDAsOCwxMzcsIDAsOCw3MywgMCw5LDI0MyxcbiAgICA4MCw3LDQsIDAsOCw4NSwgMCw4LDIxLCA4MCw4LDI1OCxcbiAgICA4Myw3LDQzLCAwLDgsMTE3LCAwLDgsNTMsIDAsOSwyMDMsXG4gICAgODEsNywxMywgMCw4LDEwMSwgMCw4LDM3LCAwLDksMTcxLFxuICAgIDAsOCw1LCAwLDgsMTMzLCAwLDgsNjksIDAsOSwyMzUsXG4gICAgODAsNyw4LCAwLDgsOTMsIDAsOCwyOSwgMCw5LDE1NSxcbiAgICA4NCw3LDgzLCAwLDgsMTI1LCAwLDgsNjEsIDAsOSwyMTksXG4gICAgODIsNywyMywgMCw4LDEwOSwgMCw4LDQ1LCAwLDksMTg3LFxuICAgIDAsOCwxMywgMCw4LDE0MSwgMCw4LDc3LCAwLDksMjUxLFxuICAgIDgwLDcsMywgMCw4LDgzLCAwLDgsMTksIDg1LDgsMTk1LFxuICAgIDgzLDcsMzUsIDAsOCwxMTUsIDAsOCw1MSwgMCw5LDE5OSxcbiAgICA4MSw3LDExLCAwLDgsOTksIDAsOCwzNSwgMCw5LDE2NyxcbiAgICAwLDgsMywgMCw4LDEzMSwgMCw4LDY3LCAwLDksMjMxLFxuICAgIDgwLDcsNywgMCw4LDkxLCAwLDgsMjcsIDAsOSwxNTEsXG4gICAgODQsNyw2NywgMCw4LDEyMywgMCw4LDU5LCAwLDksMjE1LFxuICAgIDgyLDcsMTksIDAsOCwxMDcsIDAsOCw0MywgMCw5LDE4MyxcbiAgICAwLDgsMTEsIDAsOCwxMzksIDAsOCw3NSwgMCw5LDI0NyxcbiAgICA4MCw3LDUsIDAsOCw4NywgMCw4LDIzLCAxOTIsOCwwLFxuICAgIDgzLDcsNTEsIDAsOCwxMTksIDAsOCw1NSwgMCw5LDIwNyxcbiAgICA4MSw3LDE1LCAwLDgsMTAzLCAwLDgsMzksIDAsOSwxNzUsXG4gICAgMCw4LDcsIDAsOCwxMzUsIDAsOCw3MSwgMCw5LDIzOSxcbiAgICA4MCw3LDksIDAsOCw5NSwgMCw4LDMxLCAwLDksMTU5LFxuICAgIDg0LDcsOTksIDAsOCwxMjcsIDAsOCw2MywgMCw5LDIyMyxcbiAgICA4Miw3LDI3LCAwLDgsMTExLCAwLDgsNDcsIDAsOSwxOTEsXG4gICAgMCw4LDE1LCAwLDgsMTQzLCAwLDgsNzksIDAsOSwyNTVcbl07XG52YXIgZml4ZWRfdGQgPSBbXG4gICAgODAsNSwxLCA4Nyw1LDI1NywgODMsNSwxNywgOTEsNSw0MDk3LFxuICAgIDgxLDUsNSwgODksNSwxMDI1LCA4NSw1LDY1LCA5Myw1LDE2Mzg1LFxuICAgIDgwLDUsMywgODgsNSw1MTMsIDg0LDUsMzMsIDkyLDUsODE5MyxcbiAgICA4Miw1LDksIDkwLDUsMjA0OSwgODYsNSwxMjksIDE5Miw1LDI0NTc3LFxuICAgIDgwLDUsMiwgODcsNSwzODUsIDgzLDUsMjUsIDkxLDUsNjE0NSxcbiAgICA4MSw1LDcsIDg5LDUsMTUzNywgODUsNSw5NywgOTMsNSwyNDU3NyxcbiAgICA4MCw1LDQsIDg4LDUsNzY5LCA4NCw1LDQ5LCA5Miw1LDEyMjg5LFxuICAgIDgyLDUsMTMsIDkwLDUsMzA3MywgODYsNSwxOTMsIDE5Miw1LDI0NTc3XG5dO1xuXG4gIC8vIFRhYmxlcyBmb3IgZGVmbGF0ZSBmcm9tIFBLWklQJ3MgYXBwbm90ZS50eHQuXG4gIHZhciBjcGxlbnMgPSBbIC8vIENvcHkgbGVuZ3RocyBmb3IgbGl0ZXJhbCBjb2RlcyAyNTcuLjI4NVxuICAgICAgICAzLCA0LCA1LCA2LCA3LCA4LCA5LCAxMCwgMTEsIDEzLCAxNSwgMTcsIDE5LCAyMywgMjcsIDMxLFxuICAgICAgICAzNSwgNDMsIDUxLCA1OSwgNjcsIDgzLCA5OSwgMTE1LCAxMzEsIDE2MywgMTk1LCAyMjcsIDI1OCwgMCwgMFxuICBdO1xuXG4gIC8vIHNlZSBub3RlICMxMyBhYm92ZSBhYm91dCAyNThcbiAgdmFyIGNwbGV4dCA9IFsgLy8gRXh0cmEgYml0cyBmb3IgbGl0ZXJhbCBjb2RlcyAyNTcuLjI4NVxuICAgICAgICAwLCAwLCAwLCAwLCAwLCAwLCAwLCAwLCAxLCAxLCAxLCAxLCAyLCAyLCAyLCAyLFxuICAgICAgICAzLCAzLCAzLCAzLCA0LCA0LCA0LCA0LCA1LCA1LCA1LCA1LCAwLCAxMTIsIDExMiAgLy8gMTEyPT1pbnZhbGlkXG4gIF07XG5cbiB2YXIgY3BkaXN0ID0gWyAvLyBDb3B5IG9mZnNldHMgZm9yIGRpc3RhbmNlIGNvZGVzIDAuLjI5XG4gICAgICAgIDEsIDIsIDMsIDQsIDUsIDcsIDksIDEzLCAxNywgMjUsIDMzLCA0OSwgNjUsIDk3LCAxMjksIDE5MyxcbiAgICAgICAgMjU3LCAzODUsIDUxMywgNzY5LCAxMDI1LCAxNTM3LCAyMDQ5LCAzMDczLCA0MDk3LCA2MTQ1LFxuICAgICAgICA4MTkzLCAxMjI4OSwgMTYzODUsIDI0NTc3XG4gIF07XG5cbiAgdmFyIGNwZGV4dCA9IFsgLy8gRXh0cmEgYml0cyBmb3IgZGlzdGFuY2UgY29kZXNcbiAgICAgICAgMCwgMCwgMCwgMCwgMSwgMSwgMiwgMiwgMywgMywgNCwgNCwgNSwgNSwgNiwgNixcbiAgICAgICAgNywgNywgOCwgOCwgOSwgOSwgMTAsIDEwLCAxMSwgMTEsXG4gICAgICAgIDEyLCAxMiwgMTMsIDEzXTtcblxuLy9cbi8vIFpTdHJlYW0uamF2YVxuLy9cblxuZnVuY3Rpb24gWlN0cmVhbSgpIHtcbn1cblxuXG5aU3RyZWFtLnByb3RvdHlwZS5pbmZsYXRlSW5pdCA9IGZ1bmN0aW9uKHcsIG5vd3JhcCkge1xuICAgIGlmICghdykge1xuXHR3ID0gREVGX1dCSVRTO1xuICAgIH1cbiAgICBpZiAobm93cmFwKSB7XG5cdG5vd3JhcCA9IGZhbHNlO1xuICAgIH1cbiAgICB0aGlzLmlzdGF0ZSA9IG5ldyBJbmZsYXRlKCk7XG4gICAgcmV0dXJuIHRoaXMuaXN0YXRlLmluZmxhdGVJbml0KHRoaXMsIG5vd3JhcD8tdzp3KTtcbn1cblxuWlN0cmVhbS5wcm90b3R5cGUuaW5mbGF0ZSA9IGZ1bmN0aW9uKGYpIHtcbiAgICBpZih0aGlzLmlzdGF0ZT09bnVsbCkgcmV0dXJuIFpfU1RSRUFNX0VSUk9SO1xuICAgIHJldHVybiB0aGlzLmlzdGF0ZS5pbmZsYXRlKHRoaXMsIGYpO1xufVxuXG5aU3RyZWFtLnByb3RvdHlwZS5pbmZsYXRlRW5kID0gZnVuY3Rpb24oKXtcbiAgICBpZih0aGlzLmlzdGF0ZT09bnVsbCkgcmV0dXJuIFpfU1RSRUFNX0VSUk9SO1xuICAgIHZhciByZXQ9aXN0YXRlLmluZmxhdGVFbmQodGhpcyk7XG4gICAgdGhpcy5pc3RhdGUgPSBudWxsO1xuICAgIHJldHVybiByZXQ7XG59XG5aU3RyZWFtLnByb3RvdHlwZS5pbmZsYXRlU3luYyA9IGZ1bmN0aW9uKCl7XG4gICAgLy8gaWYoaXN0YXRlID09IG51bGwpIHJldHVybiBaX1NUUkVBTV9FUlJPUjtcbiAgICByZXR1cm4gaXN0YXRlLmluZmxhdGVTeW5jKHRoaXMpO1xufVxuWlN0cmVhbS5wcm90b3R5cGUuaW5mbGF0ZVNldERpY3Rpb25hcnkgPSBmdW5jdGlvbihkaWN0aW9uYXJ5LCBkaWN0TGVuZ3RoKXtcbiAgICAvLyBpZihpc3RhdGUgPT0gbnVsbCkgcmV0dXJuIFpfU1RSRUFNX0VSUk9SO1xuICAgIHJldHVybiBpc3RhdGUuaW5mbGF0ZVNldERpY3Rpb25hcnkodGhpcywgZGljdGlvbmFyeSwgZGljdExlbmd0aCk7XG59XG5cbi8qXG5cbiAgcHVibGljIGludCBkZWZsYXRlSW5pdChpbnQgbGV2ZWwpe1xuICAgIHJldHVybiBkZWZsYXRlSW5pdChsZXZlbCwgTUFYX1dCSVRTKTtcbiAgfVxuICBwdWJsaWMgaW50IGRlZmxhdGVJbml0KGludCBsZXZlbCwgYm9vbGVhbiBub3dyYXApe1xuICAgIHJldHVybiBkZWZsYXRlSW5pdChsZXZlbCwgTUFYX1dCSVRTLCBub3dyYXApO1xuICB9XG4gIHB1YmxpYyBpbnQgZGVmbGF0ZUluaXQoaW50IGxldmVsLCBpbnQgYml0cyl7XG4gICAgcmV0dXJuIGRlZmxhdGVJbml0KGxldmVsLCBiaXRzLCBmYWxzZSk7XG4gIH1cbiAgcHVibGljIGludCBkZWZsYXRlSW5pdChpbnQgbGV2ZWwsIGludCBiaXRzLCBib29sZWFuIG5vd3JhcCl7XG4gICAgZHN0YXRlPW5ldyBEZWZsYXRlKCk7XG4gICAgcmV0dXJuIGRzdGF0ZS5kZWZsYXRlSW5pdCh0aGlzLCBsZXZlbCwgbm93cmFwPy1iaXRzOmJpdHMpO1xuICB9XG4gIHB1YmxpYyBpbnQgZGVmbGF0ZShpbnQgZmx1c2gpe1xuICAgIGlmKGRzdGF0ZT09bnVsbCl7XG4gICAgICByZXR1cm4gWl9TVFJFQU1fRVJST1I7XG4gICAgfVxuICAgIHJldHVybiBkc3RhdGUuZGVmbGF0ZSh0aGlzLCBmbHVzaCk7XG4gIH1cbiAgcHVibGljIGludCBkZWZsYXRlRW5kKCl7XG4gICAgaWYoZHN0YXRlPT1udWxsKSByZXR1cm4gWl9TVFJFQU1fRVJST1I7XG4gICAgaW50IHJldD1kc3RhdGUuZGVmbGF0ZUVuZCgpO1xuICAgIGRzdGF0ZT1udWxsO1xuICAgIHJldHVybiByZXQ7XG4gIH1cbiAgcHVibGljIGludCBkZWZsYXRlUGFyYW1zKGludCBsZXZlbCwgaW50IHN0cmF0ZWd5KXtcbiAgICBpZihkc3RhdGU9PW51bGwpIHJldHVybiBaX1NUUkVBTV9FUlJPUjtcbiAgICByZXR1cm4gZHN0YXRlLmRlZmxhdGVQYXJhbXModGhpcywgbGV2ZWwsIHN0cmF0ZWd5KTtcbiAgfVxuICBwdWJsaWMgaW50IGRlZmxhdGVTZXREaWN0aW9uYXJ5IChieXRlW10gZGljdGlvbmFyeSwgaW50IGRpY3RMZW5ndGgpe1xuICAgIGlmKGRzdGF0ZSA9PSBudWxsKVxuICAgICAgcmV0dXJuIFpfU1RSRUFNX0VSUk9SO1xuICAgIHJldHVybiBkc3RhdGUuZGVmbGF0ZVNldERpY3Rpb25hcnkodGhpcywgZGljdGlvbmFyeSwgZGljdExlbmd0aCk7XG4gIH1cblxuKi9cblxuLypcbiAgLy8gRmx1c2ggYXMgbXVjaCBwZW5kaW5nIG91dHB1dCBhcyBwb3NzaWJsZS4gQWxsIGRlZmxhdGUoKSBvdXRwdXQgZ29lc1xuICAvLyB0aHJvdWdoIHRoaXMgZnVuY3Rpb24gc28gc29tZSBhcHBsaWNhdGlvbnMgbWF5IHdpc2ggdG8gbW9kaWZ5IGl0XG4gIC8vIHRvIGF2b2lkIGFsbG9jYXRpbmcgYSBsYXJnZSBzdHJtLT5uZXh0X291dCBidWZmZXIgYW5kIGNvcHlpbmcgaW50byBpdC5cbiAgLy8gKFNlZSBhbHNvIHJlYWRfYnVmKCkpLlxuICB2b2lkIGZsdXNoX3BlbmRpbmcoKXtcbiAgICBpbnQgbGVuPWRzdGF0ZS5wZW5kaW5nO1xuXG4gICAgaWYobGVuPmF2YWlsX291dCkgbGVuPWF2YWlsX291dDtcbiAgICBpZihsZW49PTApIHJldHVybjtcblxuICAgIGlmKGRzdGF0ZS5wZW5kaW5nX2J1Zi5sZW5ndGg8PWRzdGF0ZS5wZW5kaW5nX291dCB8fFxuICAgICAgIG5leHRfb3V0Lmxlbmd0aDw9bmV4dF9vdXRfaW5kZXggfHxcbiAgICAgICBkc3RhdGUucGVuZGluZ19idWYubGVuZ3RoPChkc3RhdGUucGVuZGluZ19vdXQrbGVuKSB8fFxuICAgICAgIG5leHRfb3V0Lmxlbmd0aDwobmV4dF9vdXRfaW5kZXgrbGVuKSl7XG4gICAgICBTeXN0ZW0ub3V0LnByaW50bG4oZHN0YXRlLnBlbmRpbmdfYnVmLmxlbmd0aCtcIiwgXCIrZHN0YXRlLnBlbmRpbmdfb3V0K1xuXHRcdFx0IFwiLCBcIituZXh0X291dC5sZW5ndGgrXCIsIFwiK25leHRfb3V0X2luZGV4K1wiLCBcIitsZW4pO1xuICAgICAgU3lzdGVtLm91dC5wcmludGxuKFwiYXZhaWxfb3V0PVwiK2F2YWlsX291dCk7XG4gICAgfVxuXG4gICAgU3lzdGVtLmFycmF5Y29weShkc3RhdGUucGVuZGluZ19idWYsIGRzdGF0ZS5wZW5kaW5nX291dCxcblx0XHQgICAgIG5leHRfb3V0LCBuZXh0X291dF9pbmRleCwgbGVuKTtcblxuICAgIG5leHRfb3V0X2luZGV4Kz1sZW47XG4gICAgZHN0YXRlLnBlbmRpbmdfb3V0Kz1sZW47XG4gICAgdG90YWxfb3V0Kz1sZW47XG4gICAgYXZhaWxfb3V0LT1sZW47XG4gICAgZHN0YXRlLnBlbmRpbmctPWxlbjtcbiAgICBpZihkc3RhdGUucGVuZGluZz09MCl7XG4gICAgICBkc3RhdGUucGVuZGluZ19vdXQ9MDtcbiAgICB9XG4gIH1cblxuICAvLyBSZWFkIGEgbmV3IGJ1ZmZlciBmcm9tIHRoZSBjdXJyZW50IGlucHV0IHN0cmVhbSwgdXBkYXRlIHRoZSBhZGxlcjMyXG4gIC8vIGFuZCB0b3RhbCBudW1iZXIgb2YgYnl0ZXMgcmVhZC4gIEFsbCBkZWZsYXRlKCkgaW5wdXQgZ29lcyB0aHJvdWdoXG4gIC8vIHRoaXMgZnVuY3Rpb24gc28gc29tZSBhcHBsaWNhdGlvbnMgbWF5IHdpc2ggdG8gbW9kaWZ5IGl0IHRvIGF2b2lkXG4gIC8vIGFsbG9jYXRpbmcgYSBsYXJnZSBzdHJtLT5uZXh0X2luIGJ1ZmZlciBhbmQgY29weWluZyBmcm9tIGl0LlxuICAvLyAoU2VlIGFsc28gZmx1c2hfcGVuZGluZygpKS5cbiAgaW50IHJlYWRfYnVmKGJ5dGVbXSBidWYsIGludCBzdGFydCwgaW50IHNpemUpIHtcbiAgICBpbnQgbGVuPWF2YWlsX2luO1xuXG4gICAgaWYobGVuPnNpemUpIGxlbj1zaXplO1xuICAgIGlmKGxlbj09MCkgcmV0dXJuIDA7XG5cbiAgICBhdmFpbF9pbi09bGVuO1xuXG4gICAgaWYoZHN0YXRlLm5vaGVhZGVyPT0wKSB7XG4gICAgICBhZGxlcj1fYWRsZXIuYWRsZXIzMihhZGxlciwgbmV4dF9pbiwgbmV4dF9pbl9pbmRleCwgbGVuKTtcbiAgICB9XG4gICAgU3lzdGVtLmFycmF5Y29weShuZXh0X2luLCBuZXh0X2luX2luZGV4LCBidWYsIHN0YXJ0LCBsZW4pO1xuICAgIG5leHRfaW5faW5kZXggICs9IGxlbjtcbiAgICB0b3RhbF9pbiArPSBsZW47XG4gICAgcmV0dXJuIGxlbjtcbiAgfVxuXG4gIHB1YmxpYyB2b2lkIGZyZWUoKXtcbiAgICBuZXh0X2luPW51bGw7XG4gICAgbmV4dF9vdXQ9bnVsbDtcbiAgICBtc2c9bnVsbDtcbiAgICBfYWRsZXI9bnVsbDtcbiAgfVxufVxuKi9cblxuXG4vL1xuLy8gSW5mbGF0ZS5qYXZhXG4vL1xuXG5mdW5jdGlvbiBJbmZsYXRlKCkge1xuICAgIHRoaXMud2FzID0gWzBdO1xufVxuXG5JbmZsYXRlLnByb3RvdHlwZS5pbmZsYXRlUmVzZXQgPSBmdW5jdGlvbih6KSB7XG4gICAgaWYoeiA9PSBudWxsIHx8IHouaXN0YXRlID09IG51bGwpIHJldHVybiBaX1NUUkVBTV9FUlJPUjtcbiAgICBcbiAgICB6LnRvdGFsX2luID0gei50b3RhbF9vdXQgPSAwO1xuICAgIHoubXNnID0gbnVsbDtcbiAgICB6LmlzdGF0ZS5tb2RlID0gei5pc3RhdGUubm93cmFwIT0wID8gQkxPQ0tTIDogTUVUSE9EO1xuICAgIHouaXN0YXRlLmJsb2Nrcy5yZXNldCh6LCBudWxsKTtcbiAgICByZXR1cm4gWl9PSztcbn1cblxuSW5mbGF0ZS5wcm90b3R5cGUuaW5mbGF0ZUVuZCA9IGZ1bmN0aW9uKHope1xuICAgIGlmKHRoaXMuYmxvY2tzICE9IG51bGwpXG4gICAgICB0aGlzLmJsb2Nrcy5mcmVlKHopO1xuICAgIHRoaXMuYmxvY2tzPW51bGw7XG4gICAgcmV0dXJuIFpfT0s7XG59XG5cbkluZmxhdGUucHJvdG90eXBlLmluZmxhdGVJbml0ID0gZnVuY3Rpb24oeiwgdyl7XG4gICAgei5tc2cgPSBudWxsO1xuICAgIHRoaXMuYmxvY2tzID0gbnVsbDtcblxuICAgIC8vIGhhbmRsZSB1bmRvY3VtZW50ZWQgbm93cmFwIG9wdGlvbiAobm8gemxpYiBoZWFkZXIgb3IgY2hlY2spXG4gICAgbm93cmFwID0gMDtcbiAgICBpZih3IDwgMCl7XG4gICAgICB3ID0gLSB3O1xuICAgICAgbm93cmFwID0gMTtcbiAgICB9XG5cbiAgICAvLyBzZXQgd2luZG93IHNpemVcbiAgICBpZih3PDggfHx3PjE1KXtcbiAgICAgIHRoaXMuaW5mbGF0ZUVuZCh6KTtcbiAgICAgIHJldHVybiBaX1NUUkVBTV9FUlJPUjtcbiAgICB9XG4gICAgdGhpcy53Yml0cz13O1xuXG4gICAgei5pc3RhdGUuYmxvY2tzPW5ldyBJbmZCbG9ja3MoeiwgXG5cdFx0XHRcdCAgei5pc3RhdGUubm93cmFwIT0wID8gbnVsbCA6IHRoaXMsXG5cdFx0XHRcdCAgMTw8dyk7XG5cbiAgICAvLyByZXNldCBzdGF0ZVxuICAgIHRoaXMuaW5mbGF0ZVJlc2V0KHopO1xuICAgIHJldHVybiBaX09LO1xuICB9XG5cbkluZmxhdGUucHJvdG90eXBlLmluZmxhdGUgPSBmdW5jdGlvbih6LCBmKXtcbiAgICB2YXIgciwgYjtcblxuICAgIGlmKHogPT0gbnVsbCB8fCB6LmlzdGF0ZSA9PSBudWxsIHx8IHoubmV4dF9pbiA9PSBudWxsKVxuICAgICAgcmV0dXJuIFpfU1RSRUFNX0VSUk9SO1xuICAgIGYgPSBmID09IFpfRklOSVNIID8gWl9CVUZfRVJST1IgOiBaX09LO1xuICAgIHIgPSBaX0JVRl9FUlJPUjtcbiAgICB3aGlsZSAodHJ1ZSl7XG4gICAgICBzd2l0Y2ggKHouaXN0YXRlLm1vZGUpe1xuICAgICAgY2FzZSBNRVRIT0Q6XG5cbiAgICAgICAgaWYoei5hdmFpbF9pbj09MClyZXR1cm4gcjtyPWY7XG5cbiAgICAgICAgei5hdmFpbF9pbi0tOyB6LnRvdGFsX2luKys7XG4gICAgICAgIGlmKCgoei5pc3RhdGUubWV0aG9kID0gei5uZXh0X2luW3oubmV4dF9pbl9pbmRleCsrXSkmMHhmKSE9Wl9ERUZMQVRFRCl7XG4gICAgICAgICAgei5pc3RhdGUubW9kZSA9IEJBRDtcbiAgICAgICAgICB6Lm1zZz1cInVua25vd24gY29tcHJlc3Npb24gbWV0aG9kXCI7XG4gICAgICAgICAgei5pc3RhdGUubWFya2VyID0gNTsgICAgICAgLy8gY2FuJ3QgdHJ5IGluZmxhdGVTeW5jXG4gICAgICAgICAgYnJlYWs7XG4gICAgICAgIH1cbiAgICAgICAgaWYoKHouaXN0YXRlLm1ldGhvZD4+NCkrOD56LmlzdGF0ZS53Yml0cyl7XG4gICAgICAgICAgei5pc3RhdGUubW9kZSA9IEJBRDtcbiAgICAgICAgICB6Lm1zZz1cImludmFsaWQgd2luZG93IHNpemVcIjtcbiAgICAgICAgICB6LmlzdGF0ZS5tYXJrZXIgPSA1OyAgICAgICAvLyBjYW4ndCB0cnkgaW5mbGF0ZVN5bmNcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgfVxuICAgICAgICB6LmlzdGF0ZS5tb2RlPUZMQUc7XG4gICAgICBjYXNlIEZMQUc6XG5cbiAgICAgICAgaWYoei5hdmFpbF9pbj09MClyZXR1cm4gcjtyPWY7XG5cbiAgICAgICAgei5hdmFpbF9pbi0tOyB6LnRvdGFsX2luKys7XG4gICAgICAgIGIgPSAoei5uZXh0X2luW3oubmV4dF9pbl9pbmRleCsrXSkmMHhmZjtcblxuICAgICAgICBpZigoKCh6LmlzdGF0ZS5tZXRob2QgPDwgOCkrYikgJSAzMSkhPTApe1xuICAgICAgICAgIHouaXN0YXRlLm1vZGUgPSBCQUQ7XG4gICAgICAgICAgei5tc2cgPSBcImluY29ycmVjdCBoZWFkZXIgY2hlY2tcIjtcbiAgICAgICAgICB6LmlzdGF0ZS5tYXJrZXIgPSA1OyAgICAgICAvLyBjYW4ndCB0cnkgaW5mbGF0ZVN5bmNcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgfVxuXG4gICAgICAgIGlmKChiJlBSRVNFVF9ESUNUKT09MCl7XG4gICAgICAgICAgei5pc3RhdGUubW9kZSA9IEJMT0NLUztcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgfVxuICAgICAgICB6LmlzdGF0ZS5tb2RlID0gRElDVDQ7XG4gICAgICBjYXNlIERJQ1Q0OlxuXG4gICAgICAgIGlmKHouYXZhaWxfaW49PTApcmV0dXJuIHI7cj1mO1xuXG4gICAgICAgIHouYXZhaWxfaW4tLTsgei50b3RhbF9pbisrO1xuICAgICAgICB6LmlzdGF0ZS5uZWVkPSgoei5uZXh0X2luW3oubmV4dF9pbl9pbmRleCsrXSYweGZmKTw8MjQpJjB4ZmYwMDAwMDA7XG4gICAgICAgIHouaXN0YXRlLm1vZGU9RElDVDM7XG4gICAgICBjYXNlIERJQ1QzOlxuXG4gICAgICAgIGlmKHouYXZhaWxfaW49PTApcmV0dXJuIHI7cj1mO1xuXG4gICAgICAgIHouYXZhaWxfaW4tLTsgei50b3RhbF9pbisrO1xuICAgICAgICB6LmlzdGF0ZS5uZWVkKz0oKHoubmV4dF9pblt6Lm5leHRfaW5faW5kZXgrK10mMHhmZik8PDE2KSYweGZmMDAwMDtcbiAgICAgICAgei5pc3RhdGUubW9kZT1ESUNUMjtcbiAgICAgIGNhc2UgRElDVDI6XG5cbiAgICAgICAgaWYoei5hdmFpbF9pbj09MClyZXR1cm4gcjtyPWY7XG5cbiAgICAgICAgei5hdmFpbF9pbi0tOyB6LnRvdGFsX2luKys7XG4gICAgICAgIHouaXN0YXRlLm5lZWQrPSgoei5uZXh0X2luW3oubmV4dF9pbl9pbmRleCsrXSYweGZmKTw8OCkmMHhmZjAwO1xuICAgICAgICB6LmlzdGF0ZS5tb2RlPURJQ1QxO1xuICAgICAgY2FzZSBESUNUMTpcblxuICAgICAgICBpZih6LmF2YWlsX2luPT0wKXJldHVybiByO3I9ZjtcblxuICAgICAgICB6LmF2YWlsX2luLS07IHoudG90YWxfaW4rKztcbiAgICAgICAgei5pc3RhdGUubmVlZCArPSAoei5uZXh0X2luW3oubmV4dF9pbl9pbmRleCsrXSYweGZmKTtcbiAgICAgICAgei5hZGxlciA9IHouaXN0YXRlLm5lZWQ7XG4gICAgICAgIHouaXN0YXRlLm1vZGUgPSBESUNUMDtcbiAgICAgICAgcmV0dXJuIFpfTkVFRF9ESUNUO1xuICAgICAgY2FzZSBESUNUMDpcbiAgICAgICAgei5pc3RhdGUubW9kZSA9IEJBRDtcbiAgICAgICAgei5tc2cgPSBcIm5lZWQgZGljdGlvbmFyeVwiO1xuICAgICAgICB6LmlzdGF0ZS5tYXJrZXIgPSAwOyAgICAgICAvLyBjYW4gdHJ5IGluZmxhdGVTeW5jXG4gICAgICAgIHJldHVybiBaX1NUUkVBTV9FUlJPUjtcbiAgICAgIGNhc2UgQkxPQ0tTOlxuXG4gICAgICAgIHIgPSB6LmlzdGF0ZS5ibG9ja3MucHJvYyh6LCByKTtcbiAgICAgICAgaWYociA9PSBaX0RBVEFfRVJST1Ipe1xuICAgICAgICAgIHouaXN0YXRlLm1vZGUgPSBCQUQ7XG4gICAgICAgICAgei5pc3RhdGUubWFya2VyID0gMDsgICAgIC8vIGNhbiB0cnkgaW5mbGF0ZVN5bmNcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgfVxuICAgICAgICBpZihyID09IFpfT0spe1xuICAgICAgICAgIHIgPSBmO1xuICAgICAgICB9XG4gICAgICAgIGlmKHIgIT0gWl9TVFJFQU1fRU5EKXtcbiAgICAgICAgICByZXR1cm4gcjtcbiAgICAgICAgfVxuICAgICAgICByID0gZjtcbiAgICAgICAgei5pc3RhdGUuYmxvY2tzLnJlc2V0KHosIHouaXN0YXRlLndhcyk7XG4gICAgICAgIGlmKHouaXN0YXRlLm5vd3JhcCE9MCl7XG4gICAgICAgICAgei5pc3RhdGUubW9kZT1ET05FO1xuICAgICAgICAgIGJyZWFrO1xuICAgICAgICB9XG4gICAgICAgIHouaXN0YXRlLm1vZGU9Q0hFQ0s0O1xuICAgICAgY2FzZSBDSEVDSzQ6XG5cbiAgICAgICAgaWYoei5hdmFpbF9pbj09MClyZXR1cm4gcjtyPWY7XG5cbiAgICAgICAgei5hdmFpbF9pbi0tOyB6LnRvdGFsX2luKys7XG4gICAgICAgIHouaXN0YXRlLm5lZWQ9KCh6Lm5leHRfaW5bei5uZXh0X2luX2luZGV4KytdJjB4ZmYpPDwyNCkmMHhmZjAwMDAwMDtcbiAgICAgICAgei5pc3RhdGUubW9kZT1DSEVDSzM7XG4gICAgICBjYXNlIENIRUNLMzpcblxuICAgICAgICBpZih6LmF2YWlsX2luPT0wKXJldHVybiByO3I9ZjtcblxuICAgICAgICB6LmF2YWlsX2luLS07IHoudG90YWxfaW4rKztcbiAgICAgICAgei5pc3RhdGUubmVlZCs9KCh6Lm5leHRfaW5bei5uZXh0X2luX2luZGV4KytdJjB4ZmYpPDwxNikmMHhmZjAwMDA7XG4gICAgICAgIHouaXN0YXRlLm1vZGUgPSBDSEVDSzI7XG4gICAgICBjYXNlIENIRUNLMjpcblxuICAgICAgICBpZih6LmF2YWlsX2luPT0wKXJldHVybiByO3I9ZjtcblxuICAgICAgICB6LmF2YWlsX2luLS07IHoudG90YWxfaW4rKztcbiAgICAgICAgei5pc3RhdGUubmVlZCs9KCh6Lm5leHRfaW5bei5uZXh0X2luX2luZGV4KytdJjB4ZmYpPDw4KSYweGZmMDA7XG4gICAgICAgIHouaXN0YXRlLm1vZGUgPSBDSEVDSzE7XG4gICAgICBjYXNlIENIRUNLMTpcblxuICAgICAgICBpZih6LmF2YWlsX2luPT0wKXJldHVybiByO3I9ZjtcblxuICAgICAgICB6LmF2YWlsX2luLS07IHoudG90YWxfaW4rKztcbiAgICAgICAgei5pc3RhdGUubmVlZCs9KHoubmV4dF9pblt6Lm5leHRfaW5faW5kZXgrK10mMHhmZik7XG5cbiAgICAgICAgaWYoKCh6LmlzdGF0ZS53YXNbMF0pKSAhPSAoKHouaXN0YXRlLm5lZWQpKSl7XG4gICAgICAgICAgei5pc3RhdGUubW9kZSA9IEJBRDtcbiAgICAgICAgICB6Lm1zZyA9IFwiaW5jb3JyZWN0IGRhdGEgY2hlY2tcIjtcbiAgICAgICAgICB6LmlzdGF0ZS5tYXJrZXIgPSA1OyAgICAgICAvLyBjYW4ndCB0cnkgaW5mbGF0ZVN5bmNcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgfVxuXG4gICAgICAgIHouaXN0YXRlLm1vZGUgPSBET05FO1xuICAgICAgY2FzZSBET05FOlxuICAgICAgICByZXR1cm4gWl9TVFJFQU1fRU5EO1xuICAgICAgY2FzZSBCQUQ6XG4gICAgICAgIHJldHVybiBaX0RBVEFfRVJST1I7XG4gICAgICBkZWZhdWx0OlxuICAgICAgICByZXR1cm4gWl9TVFJFQU1fRVJST1I7XG4gICAgICB9XG4gICAgfVxuICB9XG5cblxuSW5mbGF0ZS5wcm90b3R5cGUuaW5mbGF0ZVNldERpY3Rpb25hcnkgPSBmdW5jdGlvbih6LCAgZGljdGlvbmFyeSwgZGljdExlbmd0aCkge1xuICAgIHZhciBpbmRleD0wO1xuICAgIHZhciBsZW5ndGggPSBkaWN0TGVuZ3RoO1xuICAgIGlmKHo9PW51bGwgfHwgei5pc3RhdGUgPT0gbnVsbHx8IHouaXN0YXRlLm1vZGUgIT0gRElDVDApXG4gICAgICByZXR1cm4gWl9TVFJFQU1fRVJST1I7XG5cbiAgICBpZih6Ll9hZGxlci5hZGxlcjMyKDEsIGRpY3Rpb25hcnksIDAsIGRpY3RMZW5ndGgpIT16LmFkbGVyKXtcbiAgICAgIHJldHVybiBaX0RBVEFfRVJST1I7XG4gICAgfVxuXG4gICAgei5hZGxlciA9IHouX2FkbGVyLmFkbGVyMzIoMCwgbnVsbCwgMCwgMCk7XG5cbiAgICBpZihsZW5ndGggPj0gKDE8PHouaXN0YXRlLndiaXRzKSl7XG4gICAgICBsZW5ndGggPSAoMTw8ei5pc3RhdGUud2JpdHMpLTE7XG4gICAgICBpbmRleD1kaWN0TGVuZ3RoIC0gbGVuZ3RoO1xuICAgIH1cbiAgICB6LmlzdGF0ZS5ibG9ja3Muc2V0X2RpY3Rpb25hcnkoZGljdGlvbmFyeSwgaW5kZXgsIGxlbmd0aCk7XG4gICAgei5pc3RhdGUubW9kZSA9IEJMT0NLUztcbiAgICByZXR1cm4gWl9PSztcbiAgfVxuXG4vLyAgc3RhdGljIHByaXZhdGUgYnl0ZVtdIG1hcmsgPSB7KGJ5dGUpMCwgKGJ5dGUpMCwgKGJ5dGUpMHhmZiwgKGJ5dGUpMHhmZn07XG52YXIgbWFyayA9IFswLCAwLCAyNTUsIDI1NV1cblxuSW5mbGF0ZS5wcm90b3R5cGUuaW5mbGF0ZVN5bmMgPSBmdW5jdGlvbih6KXtcbiAgICB2YXIgbjsgICAgICAgLy8gbnVtYmVyIG9mIGJ5dGVzIHRvIGxvb2sgYXRcbiAgICB2YXIgcDsgICAgICAgLy8gcG9pbnRlciB0byBieXRlc1xuICAgIHZhciBtOyAgICAgICAvLyBudW1iZXIgb2YgbWFya2VyIGJ5dGVzIGZvdW5kIGluIGEgcm93XG4gICAgdmFyIHIsIHc7ICAgLy8gdGVtcG9yYXJpZXMgdG8gc2F2ZSB0b3RhbF9pbiBhbmQgdG90YWxfb3V0XG5cbiAgICAvLyBzZXQgdXBcbiAgICBpZih6ID09IG51bGwgfHwgei5pc3RhdGUgPT0gbnVsbClcbiAgICAgIHJldHVybiBaX1NUUkVBTV9FUlJPUjtcbiAgICBpZih6LmlzdGF0ZS5tb2RlICE9IEJBRCl7XG4gICAgICB6LmlzdGF0ZS5tb2RlID0gQkFEO1xuICAgICAgei5pc3RhdGUubWFya2VyID0gMDtcbiAgICB9XG4gICAgaWYoKG49ei5hdmFpbF9pbik9PTApXG4gICAgICByZXR1cm4gWl9CVUZfRVJST1I7XG4gICAgcD16Lm5leHRfaW5faW5kZXg7XG4gICAgbT16LmlzdGF0ZS5tYXJrZXI7XG5cbiAgICAvLyBzZWFyY2hcbiAgICB3aGlsZSAobiE9MCAmJiBtIDwgNCl7XG4gICAgICBpZih6Lm5leHRfaW5bcF0gPT0gbWFya1ttXSl7XG4gICAgICAgIG0rKztcbiAgICAgIH1cbiAgICAgIGVsc2UgaWYoei5uZXh0X2luW3BdIT0wKXtcbiAgICAgICAgbSA9IDA7XG4gICAgICB9XG4gICAgICBlbHNle1xuICAgICAgICBtID0gNCAtIG07XG4gICAgICB9XG4gICAgICBwKys7IG4tLTtcbiAgICB9XG5cbiAgICAvLyByZXN0b3JlXG4gICAgei50b3RhbF9pbiArPSBwLXoubmV4dF9pbl9pbmRleDtcbiAgICB6Lm5leHRfaW5faW5kZXggPSBwO1xuICAgIHouYXZhaWxfaW4gPSBuO1xuICAgIHouaXN0YXRlLm1hcmtlciA9IG07XG5cbiAgICAvLyByZXR1cm4gbm8gam95IG9yIHNldCB1cCB0byByZXN0YXJ0IG9uIGEgbmV3IGJsb2NrXG4gICAgaWYobSAhPSA0KXtcbiAgICAgIHJldHVybiBaX0RBVEFfRVJST1I7XG4gICAgfVxuICAgIHI9ei50b3RhbF9pbjsgIHc9ei50b3RhbF9vdXQ7XG4gICAgdGhpcy5pbmZsYXRlUmVzZXQoeik7XG4gICAgei50b3RhbF9pbj1yOyAgei50b3RhbF9vdXQgPSB3O1xuICAgIHouaXN0YXRlLm1vZGUgPSBCTE9DS1M7XG4gICAgcmV0dXJuIFpfT0s7XG59XG5cbiAgLy8gUmV0dXJucyB0cnVlIGlmIGluZmxhdGUgaXMgY3VycmVudGx5IGF0IHRoZSBlbmQgb2YgYSBibG9jayBnZW5lcmF0ZWRcbiAgLy8gYnkgWl9TWU5DX0ZMVVNIIG9yIFpfRlVMTF9GTFVTSC4gVGhpcyBmdW5jdGlvbiBpcyB1c2VkIGJ5IG9uZSBQUFBcbiAgLy8gaW1wbGVtZW50YXRpb24gdG8gcHJvdmlkZSBhbiBhZGRpdGlvbmFsIHNhZmV0eSBjaGVjay4gUFBQIHVzZXMgWl9TWU5DX0ZMVVNIXG4gIC8vIGJ1dCByZW1vdmVzIHRoZSBsZW5ndGggYnl0ZXMgb2YgdGhlIHJlc3VsdGluZyBlbXB0eSBzdG9yZWQgYmxvY2suIFdoZW5cbiAgLy8gZGVjb21wcmVzc2luZywgUFBQIGNoZWNrcyB0aGF0IGF0IHRoZSBlbmQgb2YgaW5wdXQgcGFja2V0LCBpbmZsYXRlIGlzXG4gIC8vIHdhaXRpbmcgZm9yIHRoZXNlIGxlbmd0aCBieXRlcy5cbkluZmxhdGUucHJvdG90eXBlLmluZmxhdGVTeW5jUG9pbnQgPSBmdW5jdGlvbih6KXtcbiAgICBpZih6ID09IG51bGwgfHwgei5pc3RhdGUgPT0gbnVsbCB8fCB6LmlzdGF0ZS5ibG9ja3MgPT0gbnVsbClcbiAgICAgIHJldHVybiBaX1NUUkVBTV9FUlJPUjtcbiAgICByZXR1cm4gei5pc3RhdGUuYmxvY2tzLnN5bmNfcG9pbnQoKTtcbn1cblxuXG4vL1xuLy8gSW5mQmxvY2tzLmphdmFcbi8vXG5cbnZhciBJTkZCTE9DS1NfQk9SREVSID0gWzE2LCAxNywgMTgsIDAsIDgsIDcsIDksIDYsIDEwLCA1LCAxMSwgNCwgMTIsIDMsIDEzLCAyLCAxNCwgMSwgMTVdO1xuXG5mdW5jdGlvbiBJbmZCbG9ja3MoeiwgY2hlY2tmbiwgdykge1xuICAgIHRoaXMuaHVmdHM9bmV3IEludDMyQXJyYXkoTUFOWSozKTtcbiAgICB0aGlzLndpbmRvdz1uZXcgVWludDhBcnJheSh3KTtcbiAgICB0aGlzLmVuZD13O1xuICAgIHRoaXMuY2hlY2tmbiA9IGNoZWNrZm47XG4gICAgdGhpcy5tb2RlID0gSUJfVFlQRTtcbiAgICB0aGlzLnJlc2V0KHosIG51bGwpO1xuXG4gICAgdGhpcy5sZWZ0ID0gMDsgICAgICAgICAgICAvLyBpZiBTVE9SRUQsIGJ5dGVzIGxlZnQgdG8gY29weSBcblxuICAgIHRoaXMudGFibGUgPSAwOyAgICAgICAgICAgLy8gdGFibGUgbGVuZ3RocyAoMTQgYml0cykgXG4gICAgdGhpcy5pbmRleCA9IDA7ICAgICAgICAgICAvLyBpbmRleCBpbnRvIGJsZW5zIChvciBib3JkZXIpIFxuICAgIHRoaXMuYmxlbnMgPSBudWxsOyAgICAgICAgIC8vIGJpdCBsZW5ndGhzIG9mIGNvZGVzIFxuICAgIHRoaXMuYmI9bmV3IEludDMyQXJyYXkoMSk7IC8vIGJpdCBsZW5ndGggdHJlZSBkZXB0aCBcbiAgICB0aGlzLnRiPW5ldyBJbnQzMkFycmF5KDEpOyAvLyBiaXQgbGVuZ3RoIGRlY29kaW5nIHRyZWUgXG5cbiAgICB0aGlzLmNvZGVzID0gbmV3IEluZkNvZGVzKCk7XG5cbiAgICB0aGlzLmxhc3QgPSAwOyAgICAgICAgICAgIC8vIHRydWUgaWYgdGhpcyBibG9jayBpcyB0aGUgbGFzdCBibG9jayBcblxuICAvLyBtb2RlIGluZGVwZW5kZW50IGluZm9ybWF0aW9uIFxuICAgIHRoaXMuYml0ayA9IDA7ICAgICAgICAgICAgLy8gYml0cyBpbiBiaXQgYnVmZmVyIFxuICAgIHRoaXMuYml0YiA9IDA7ICAgICAgICAgICAgLy8gYml0IGJ1ZmZlciBcbiAgICB0aGlzLnJlYWQgPSAwOyAgICAgICAgICAgIC8vIHdpbmRvdyByZWFkIHBvaW50ZXIgXG4gICAgdGhpcy53cml0ZSA9IDA7ICAgICAgICAgICAvLyB3aW5kb3cgd3JpdGUgcG9pbnRlciBcbiAgICB0aGlzLmNoZWNrID0gMDsgICAgICAgICAgLy8gY2hlY2sgb24gb3V0cHV0IFxuXG4gICAgdGhpcy5pbmZ0cmVlPW5ldyBJbmZUcmVlKCk7XG59XG5cblxuXG5cbkluZkJsb2Nrcy5wcm90b3R5cGUucmVzZXQgPSBmdW5jdGlvbih6LCBjKXtcbiAgICBpZihjKSBjWzBdPXRoaXMuY2hlY2s7XG4gICAgaWYodGhpcy5tb2RlPT1JQl9DT0RFUyl7XG4gICAgICB0aGlzLmNvZGVzLmZyZWUoeik7XG4gICAgfVxuICAgIHRoaXMubW9kZT1JQl9UWVBFO1xuICAgIHRoaXMuYml0az0wO1xuICAgIHRoaXMuYml0Yj0wO1xuICAgIHRoaXMucmVhZD10aGlzLndyaXRlPTA7XG5cbiAgICBpZih0aGlzLmNoZWNrZm4pXG4gICAgICB6LmFkbGVyPXRoaXMuY2hlY2s9ei5fYWRsZXIuYWRsZXIzMigwLCBudWxsLCAwLCAwKTtcbiAgfVxuXG4gSW5mQmxvY2tzLnByb3RvdHlwZS5wcm9jID0gZnVuY3Rpb24oeiwgcil7XG4gICAgdmFyIHQ7ICAgICAgICAgICAgICAvLyB0ZW1wb3Jhcnkgc3RvcmFnZVxuICAgIHZhciBiOyAgICAgICAgICAgICAgLy8gYml0IGJ1ZmZlclxuICAgIHZhciBrOyAgICAgICAgICAgICAgLy8gYml0cyBpbiBiaXQgYnVmZmVyXG4gICAgdmFyIHA7ICAgICAgICAgICAgICAvLyBpbnB1dCBkYXRhIHBvaW50ZXJcbiAgICB2YXIgbjsgICAgICAgICAgICAgIC8vIGJ5dGVzIGF2YWlsYWJsZSB0aGVyZVxuICAgIHZhciBxOyAgICAgICAgICAgICAgLy8gb3V0cHV0IHdpbmRvdyB3cml0ZSBwb2ludGVyXG4gICAgdmFyIG07ICAgICAgICAgICAgICAvLyBieXRlcyB0byBlbmQgb2Ygd2luZG93IG9yIHJlYWQgcG9pbnRlclxuXG4gICAgLy8gY29weSBpbnB1dC9vdXRwdXQgaW5mb3JtYXRpb24gdG8gbG9jYWxzIChVUERBVEUgbWFjcm8gcmVzdG9yZXMpXG4gICAge3A9ei5uZXh0X2luX2luZGV4O249ei5hdmFpbF9pbjtiPXRoaXMuYml0YjtrPXRoaXMuYml0azt9XG4gICAge3E9dGhpcy53cml0ZTttPShxPHRoaXMucmVhZCA/IHRoaXMucmVhZC1xLTEgOiB0aGlzLmVuZC1xKTt9XG5cbiAgICAvLyBwcm9jZXNzIGlucHV0IGJhc2VkIG9uIGN1cnJlbnQgc3RhdGVcbiAgICB3aGlsZSh0cnVlKXtcbiAgICAgIHN3aXRjaCAodGhpcy5tb2RlKXtcbiAgICAgIGNhc2UgSUJfVFlQRTpcblxuXHR3aGlsZShrPCgzKSl7XG5cdCAgaWYobiE9MCl7XG5cdCAgICByPVpfT0s7XG5cdCAgfVxuXHQgIGVsc2V7XG5cdCAgICB0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHQgICAgei5hdmFpbF9pbj1uO1xuXHQgICAgei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgICB0aGlzLndyaXRlPXE7XG5cdCAgICByZXR1cm4gdGhpcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdCAgfTtcblx0ICBuLS07XG5cdCAgYnw9KHoubmV4dF9pbltwKytdJjB4ZmYpPDxrO1xuXHQgIGsrPTg7XG5cdH1cblx0dCA9IChiICYgNyk7XG5cdHRoaXMubGFzdCA9IHQgJiAxO1xuXG5cdHN3aXRjaCAodCA+Pj4gMSl7XG4gICAgICAgIGNhc2UgMDogICAgICAgICAgICAgICAgICAgICAgICAgLy8gc3RvcmVkIFxuICAgICAgICAgIHtiPj4+PSgzKTtrLT0oMyk7fVxuICAgICAgICAgIHQgPSBrICYgNzsgICAgICAgICAgICAgICAgICAgIC8vIGdvIHRvIGJ5dGUgYm91bmRhcnlcblxuICAgICAgICAgIHtiPj4+PSh0KTtrLT0odCk7fVxuICAgICAgICAgIHRoaXMubW9kZSA9IElCX0xFTlM7ICAgICAgICAgICAgICAgICAgLy8gZ2V0IGxlbmd0aCBvZiBzdG9yZWQgYmxvY2tcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgY2FzZSAxOiAgICAgICAgICAgICAgICAgICAgICAgICAvLyBmaXhlZFxuICAgICAgICAgIHtcbiAgICAgICAgICAgICAgdmFyIGJsPW5ldyBJbnQzMkFycmF5KDEpO1xuXHQgICAgICB2YXIgYmQ9bmV3IEludDMyQXJyYXkoMSk7XG4gICAgICAgICAgICAgIHZhciB0bD1bXTtcblx0ICAgICAgdmFyIHRkPVtdO1xuXG5cdCAgICAgIGluZmxhdGVfdHJlZXNfZml4ZWQoYmwsIGJkLCB0bCwgdGQsIHopO1xuICAgICAgICAgICAgICB0aGlzLmNvZGVzLmluaXQoYmxbMF0sIGJkWzBdLCB0bFswXSwgMCwgdGRbMF0sIDAsIHopO1xuICAgICAgICAgIH1cblxuICAgICAgICAgIHtiPj4+PSgzKTtrLT0oMyk7fVxuXG4gICAgICAgICAgdGhpcy5tb2RlID0gSUJfQ09ERVM7XG4gICAgICAgICAgYnJlYWs7XG4gICAgICAgIGNhc2UgMjogICAgICAgICAgICAgICAgICAgICAgICAgLy8gZHluYW1pY1xuXG4gICAgICAgICAge2I+Pj49KDMpO2stPSgzKTt9XG5cbiAgICAgICAgICB0aGlzLm1vZGUgPSBJQl9UQUJMRTtcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgY2FzZSAzOiAgICAgICAgICAgICAgICAgICAgICAgICAvLyBpbGxlZ2FsXG5cbiAgICAgICAgICB7Yj4+Pj0oMyk7ay09KDMpO31cbiAgICAgICAgICB0aGlzLm1vZGUgPSBCQUQ7XG4gICAgICAgICAgei5tc2cgPSBcImludmFsaWQgYmxvY2sgdHlwZVwiO1xuICAgICAgICAgIHIgPSBaX0RBVEFfRVJST1I7XG5cblx0ICB0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHQgIHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICB0aGlzLndyaXRlPXE7XG5cdCAgcmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHR9XG5cdGJyZWFrO1xuICAgICAgY2FzZSBJQl9MRU5TOlxuXHR3aGlsZShrPCgzMikpe1xuXHQgIGlmKG4hPTApe1xuXHQgICAgcj1aX09LO1xuXHQgIH1cblx0ICBlbHNle1xuXHQgICAgdGhpcy5iaXRiPWI7IHRoaXMuYml0az1rOyBcblx0ICAgIHouYXZhaWxfaW49bjtcblx0ICAgIHoudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgICAgdGhpcy53cml0ZT1xO1xuXHQgICAgcmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgIH07XG5cdCAgbi0tO1xuXHQgIGJ8PSh6Lm5leHRfaW5bcCsrXSYweGZmKTw8aztcblx0ICBrKz04O1xuXHR9XG5cblx0aWYgKCgoKH5iKSA+Pj4gMTYpICYgMHhmZmZmKSAhPSAoYiAmIDB4ZmZmZikpe1xuXHQgIHRoaXMubW9kZSA9IEJBRDtcblx0ICB6Lm1zZyA9IFwiaW52YWxpZCBzdG9yZWQgYmxvY2sgbGVuZ3Roc1wiO1xuXHQgIHIgPSBaX0RBVEFfRVJST1I7XG5cblx0ICB0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHQgIHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICB0aGlzLndyaXRlPXE7XG5cdCAgcmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHR9XG5cdHRoaXMubGVmdCA9IChiICYgMHhmZmZmKTtcblx0YiA9IGsgPSAwOyAgICAgICAgICAgICAgICAgICAgICAgLy8gZHVtcCBiaXRzXG5cdHRoaXMubW9kZSA9IHRoaXMubGVmdCE9MCA/IElCX1NUT1JFRCA6ICh0aGlzLmxhc3QhPTAgPyBJQl9EUlkgOiBJQl9UWVBFKTtcblx0YnJlYWs7XG4gICAgICBjYXNlIElCX1NUT1JFRDpcblx0aWYgKG4gPT0gMCl7XG5cdCAgdGhpcy5iaXRiPWI7IHRoaXMuYml0az1rOyBcblx0ICB6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgd3JpdGU9cTtcblx0ICByZXR1cm4gdGhpcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdH1cblxuXHRpZihtPT0wKXtcblx0ICBpZihxPT1lbmQmJnJlYWQhPTApe1xuXHQgICAgcT0wOyBtPShxPHRoaXMucmVhZCA/IHRoaXMucmVhZC1xLTEgOiB0aGlzLmVuZC1xKTtcblx0ICB9XG5cdCAgaWYobT09MCl7XG5cdCAgICB0aGlzLndyaXRlPXE7IFxuXHQgICAgcj10aGlzLmluZmxhdGVfZmx1c2goeixyKTtcblx0ICAgIHE9dGhpcy53cml0ZTsgbSA9IChxIDwgdGhpcy5yZWFkID8gdGhpcy5yZWFkLXEtMSA6IHRoaXMuZW5kLXEpO1xuXHQgICAgaWYocT09dGhpcy5lbmQgJiYgdGhpcy5yZWFkICE9IDApe1xuXHQgICAgICBxPTA7IG0gPSAocSA8IHRoaXMucmVhZCA/IHRoaXMucmVhZC1xLTEgOiB0aGlzLmVuZC1xKTtcblx0ICAgIH1cblx0ICAgIGlmKG09PTApe1xuXHQgICAgICB0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHQgICAgICB6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgICAgIHRoaXMud3JpdGU9cTtcblx0ICAgICAgcmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgICAgfVxuXHQgIH1cblx0fVxuXHRyPVpfT0s7XG5cblx0dCA9IHRoaXMubGVmdDtcblx0aWYodD5uKSB0ID0gbjtcblx0aWYodD5tKSB0ID0gbTtcblx0YXJyYXlDb3B5KHoubmV4dF9pbiwgcCwgd2luZG93LCBxLCB0KTtcblx0cCArPSB0OyAgbiAtPSB0O1xuXHRxICs9IHQ7ICBtIC09IHQ7XG5cdGlmICgodGhpcy5sZWZ0IC09IHQpICE9IDApXG5cdCAgYnJlYWs7XG5cdHRoaXMubW9kZSA9ICh0aGlzLmxhc3QgIT0gMCA/IElCX0RSWSA6IElCX1RZUEUpO1xuXHRicmVhaztcbiAgICAgIGNhc2UgSUJfVEFCTEU6XG5cblx0d2hpbGUoazwoMTQpKXtcblx0ICBpZihuIT0wKXtcblx0ICAgIHI9Wl9PSztcblx0ICB9XG5cdCAgZWxzZXtcblx0ICAgIHRoaXMuYml0Yj1iOyB0aGlzLmJpdGs9azsgXG5cdCAgICB6LmF2YWlsX2luPW47XG5cdCAgICB6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICAgIHRoaXMud3JpdGU9cTtcblx0ICAgIHJldHVybiB0aGlzLmluZmxhdGVfZmx1c2goeixyKTtcblx0ICB9O1xuXHQgIG4tLTtcblx0ICBifD0oei5uZXh0X2luW3ArK10mMHhmZik8PGs7XG5cdCAgays9ODtcblx0fVxuXG5cdHRoaXMudGFibGUgPSB0ID0gKGIgJiAweDNmZmYpO1xuXHRpZiAoKHQgJiAweDFmKSA+IDI5IHx8ICgodCA+PiA1KSAmIDB4MWYpID4gMjkpXG5cdCAge1xuXHQgICAgdGhpcy5tb2RlID0gSUJfQkFEO1xuXHQgICAgei5tc2cgPSBcInRvbyBtYW55IGxlbmd0aCBvciBkaXN0YW5jZSBzeW1ib2xzXCI7XG5cdCAgICByID0gWl9EQVRBX0VSUk9SO1xuXG5cdCAgICB0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHQgICAgei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgICAgdGhpcy53cml0ZT1xO1xuXHQgICAgcmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgIH1cblx0dCA9IDI1OCArICh0ICYgMHgxZikgKyAoKHQgPj4gNSkgJiAweDFmKTtcblx0aWYodGhpcy5ibGVucz09bnVsbCB8fCB0aGlzLmJsZW5zLmxlbmd0aDx0KXtcblx0ICAgIHRoaXMuYmxlbnM9bmV3IEludDMyQXJyYXkodCk7XG5cdH1cblx0ZWxzZXtcblx0ICBmb3IodmFyIGk9MDsgaTx0OyBpKyspe1xuICAgICAgICAgICAgICB0aGlzLmJsZW5zW2ldPTA7XG4gICAgICAgICAgfVxuXHR9XG5cblx0e2I+Pj49KDE0KTtrLT0oMTQpO31cblxuXHR0aGlzLmluZGV4ID0gMDtcblx0bW9kZSA9IElCX0JUUkVFO1xuICAgICAgY2FzZSBJQl9CVFJFRTpcblx0d2hpbGUgKHRoaXMuaW5kZXggPCA0ICsgKHRoaXMudGFibGUgPj4+IDEwKSl7XG5cdCAgd2hpbGUoazwoMykpe1xuXHQgICAgaWYobiE9MCl7XG5cdCAgICAgIHI9Wl9PSztcblx0ICAgIH1cblx0ICAgIGVsc2V7XG5cdCAgICAgIHRoaXMuYml0Yj1iOyB0aGlzLmJpdGs9azsgXG5cdCAgICAgIHouYXZhaWxfaW49bjtcblx0ICAgICAgei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgICAgIHRoaXMud3JpdGU9cTtcblx0ICAgICAgcmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgICAgfTtcblx0ICAgIG4tLTtcblx0ICAgIGJ8PSh6Lm5leHRfaW5bcCsrXSYweGZmKTw8aztcblx0ICAgIGsrPTg7XG5cdCAgfVxuXG5cdCAgdGhpcy5ibGVuc1tJTkZCTE9DS1NfQk9SREVSW3RoaXMuaW5kZXgrK11dID0gYiY3O1xuXG5cdCAge2I+Pj49KDMpO2stPSgzKTt9XG5cdH1cblxuXHR3aGlsZSh0aGlzLmluZGV4IDwgMTkpe1xuXHQgIHRoaXMuYmxlbnNbSU5GQkxPQ0tTX0JPUkRFUlt0aGlzLmluZGV4KytdXSA9IDA7XG5cdH1cblxuXHR0aGlzLmJiWzBdID0gNztcblx0dCA9IHRoaXMuaW5mdHJlZS5pbmZsYXRlX3RyZWVzX2JpdHModGhpcy5ibGVucywgdGhpcy5iYiwgdGhpcy50YiwgdGhpcy5odWZ0cywgeik7XG5cdGlmICh0ICE9IFpfT0spe1xuXHQgIHIgPSB0O1xuXHQgIGlmIChyID09IFpfREFUQV9FUlJPUil7XG5cdCAgICB0aGlzLmJsZW5zPW51bGw7XG5cdCAgICB0aGlzLm1vZGUgPSBJQl9CQUQ7XG5cdCAgfVxuXG5cdCAgdGhpcy5iaXRiPWI7IHRoaXMuYml0az1rOyBcblx0ICB6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgd3JpdGU9cTtcblx0ICByZXR1cm4gdGhpcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdH1cblxuXHR0aGlzLmluZGV4ID0gMDtcblx0dGhpcy5tb2RlID0gSUJfRFRSRUU7XG4gICAgICBjYXNlIElCX0RUUkVFOlxuXHR3aGlsZSAodHJ1ZSl7XG5cdCAgdCA9IHRoaXMudGFibGU7XG5cdCAgaWYoISh0aGlzLmluZGV4IDwgMjU4ICsgKHQgJiAweDFmKSArICgodCA+PiA1KSAmIDB4MWYpKSl7XG5cdCAgICBicmVhaztcblx0ICB9XG5cblx0ICB2YXIgaDsgLy9pbnRbXVxuXHQgIHZhciBpLCBqLCBjO1xuXG5cdCAgdCA9IHRoaXMuYmJbMF07XG5cblx0ICB3aGlsZShrPCh0KSl7XG5cdCAgICBpZihuIT0wKXtcblx0ICAgICAgcj1aX09LO1xuXHQgICAgfVxuXHQgICAgZWxzZXtcblx0ICAgICAgdGhpcy5iaXRiPWI7IHRoaXMuYml0az1rOyBcblx0ICAgICAgei5hdmFpbF9pbj1uO1xuXHQgICAgICB6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICAgICAgdGhpcy53cml0ZT1xO1xuXHQgICAgICByZXR1cm4gdGhpcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdCAgICB9O1xuXHQgICAgbi0tO1xuXHQgICAgYnw9KHoubmV4dF9pbltwKytdJjB4ZmYpPDxrO1xuXHQgICAgays9ODtcblx0ICB9XG5cbi8vXHQgIGlmICh0aGlzLnRiWzBdPT0tMSl7XG4vLyAgICAgICAgICAgIGRsb2coXCJudWxsLi4uXCIpO1xuLy9cdCAgfVxuXG5cdCAgdD10aGlzLmh1ZnRzWyh0aGlzLnRiWzBdKyhiICYgaW5mbGF0ZV9tYXNrW3RdKSkqMysxXTtcblx0ICBjPXRoaXMuaHVmdHNbKHRoaXMudGJbMF0rKGIgJiBpbmZsYXRlX21hc2tbdF0pKSozKzJdO1xuXG5cdCAgaWYgKGMgPCAxNil7XG5cdCAgICBiPj4+PSh0KTtrLT0odCk7XG5cdCAgICB0aGlzLmJsZW5zW3RoaXMuaW5kZXgrK10gPSBjO1xuXHQgIH1cblx0ICBlbHNlIHsgLy8gYyA9PSAxNi4uMThcblx0ICAgIGkgPSBjID09IDE4ID8gNyA6IGMgLSAxNDtcblx0ICAgIGogPSBjID09IDE4ID8gMTEgOiAzO1xuXG5cdCAgICB3aGlsZShrPCh0K2kpKXtcblx0ICAgICAgaWYobiE9MCl7XG5cdFx0cj1aX09LO1xuXHQgICAgICB9XG5cdCAgICAgIGVsc2V7XG5cdFx0dGhpcy5iaXRiPWI7IHRoaXMuYml0az1rOyBcblx0XHR6LmF2YWlsX2luPW47XG5cdFx0ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdFx0dGhpcy53cml0ZT1xO1xuXHRcdHJldHVybiB0aGlzLmluZmxhdGVfZmx1c2goeixyKTtcblx0ICAgICAgfTtcblx0ICAgICAgbi0tO1xuXHQgICAgICBifD0oei5uZXh0X2luW3ArK10mMHhmZik8PGs7XG5cdCAgICAgIGsrPTg7XG5cdCAgICB9XG5cblx0ICAgIGI+Pj49KHQpO2stPSh0KTtcblxuXHQgICAgaiArPSAoYiAmIGluZmxhdGVfbWFza1tpXSk7XG5cblx0ICAgIGI+Pj49KGkpO2stPShpKTtcblxuXHQgICAgaSA9IHRoaXMuaW5kZXg7XG5cdCAgICB0ID0gdGhpcy50YWJsZTtcblx0ICAgIGlmIChpICsgaiA+IDI1OCArICh0ICYgMHgxZikgKyAoKHQgPj4gNSkgJiAweDFmKSB8fFxuXHRcdChjID09IDE2ICYmIGkgPCAxKSl7XG5cdCAgICAgIHRoaXMuYmxlbnM9bnVsbDtcblx0ICAgICAgdGhpcy5tb2RlID0gSUJfQkFEO1xuXHQgICAgICB6Lm1zZyA9IFwiaW52YWxpZCBiaXQgbGVuZ3RoIHJlcGVhdFwiO1xuXHQgICAgICByID0gWl9EQVRBX0VSUk9SO1xuXG5cdCAgICAgIHRoaXMuYml0Yj1iOyB0aGlzLmJpdGs9azsgXG5cdCAgICAgIHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICAgICAgdGhpcy53cml0ZT1xO1xuXHQgICAgICByZXR1cm4gdGhpcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdCAgICB9XG5cblx0ICAgIGMgPSBjID09IDE2ID8gdGhpcy5ibGVuc1tpLTFdIDogMDtcblx0ICAgIGRve1xuXHQgICAgICB0aGlzLmJsZW5zW2krK10gPSBjO1xuXHQgICAgfVxuXHQgICAgd2hpbGUgKC0taiE9MCk7XG5cdCAgICB0aGlzLmluZGV4ID0gaTtcblx0ICB9XG5cdH1cblxuXHR0aGlzLnRiWzBdPS0xO1xuXHR7XG5cdCAgICB2YXIgYmw9bmV3IEludDMyQXJyYXkoMSk7XG5cdCAgICB2YXIgYmQ9bmV3IEludDMyQXJyYXkoMSk7XG5cdCAgICB2YXIgdGw9bmV3IEludDMyQXJyYXkoMSk7XG5cdCAgICB2YXIgdGQ9bmV3IEludDMyQXJyYXkoMSk7XG5cdCAgICBibFswXSA9IDk7ICAgICAgICAgLy8gbXVzdCBiZSA8PSA5IGZvciBsb29rYWhlYWQgYXNzdW1wdGlvbnNcblx0ICAgIGJkWzBdID0gNjsgICAgICAgICAvLyBtdXN0IGJlIDw9IDkgZm9yIGxvb2thaGVhZCBhc3N1bXB0aW9uc1xuXG5cdCAgICB0ID0gdGhpcy50YWJsZTtcblx0ICAgIHQgPSB0aGlzLmluZnRyZWUuaW5mbGF0ZV90cmVlc19keW5hbWljKDI1NyArICh0ICYgMHgxZiksIFxuXHRcdFx0XHRcdCAgICAgIDEgKyAoKHQgPj4gNSkgJiAweDFmKSxcblx0XHRcdFx0XHQgICAgICB0aGlzLmJsZW5zLCBibCwgYmQsIHRsLCB0ZCwgdGhpcy5odWZ0cywgeik7XG5cblx0ICAgIGlmICh0ICE9IFpfT0spe1xuXHQgICAgICAgIGlmICh0ID09IFpfREFUQV9FUlJPUil7XG5cdCAgICAgICAgICAgIHRoaXMuYmxlbnM9bnVsbDtcblx0ICAgICAgICAgICAgdGhpcy5tb2RlID0gQkFEO1xuXHQgICAgICAgIH1cblx0ICAgICAgICByID0gdDtcblxuXHQgICAgICAgIHRoaXMuYml0Yj1iOyB0aGlzLmJpdGs9azsgXG5cdCAgICAgICAgei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgICAgICAgIHRoaXMud3JpdGU9cTtcblx0ICAgICAgICByZXR1cm4gdGhpcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdCAgICB9XG5cdCAgICB0aGlzLmNvZGVzLmluaXQoYmxbMF0sIGJkWzBdLCB0aGlzLmh1ZnRzLCB0bFswXSwgdGhpcy5odWZ0cywgdGRbMF0sIHopO1xuXHR9XG5cdHRoaXMubW9kZSA9IElCX0NPREVTO1xuICAgICAgY2FzZSBJQl9DT0RFUzpcblx0dGhpcy5iaXRiPWI7IHRoaXMuYml0az1rO1xuXHR6LmF2YWlsX2luPW47IHoudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHR0aGlzLndyaXRlPXE7XG5cblx0aWYgKChyID0gdGhpcy5jb2Rlcy5wcm9jKHRoaXMsIHosIHIpKSAhPSBaX1NUUkVBTV9FTkQpe1xuXHQgIHJldHVybiB0aGlzLmluZmxhdGVfZmx1c2goeiwgcik7XG5cdH1cblx0ciA9IFpfT0s7XG5cdHRoaXMuY29kZXMuZnJlZSh6KTtcblxuXHRwPXoubmV4dF9pbl9pbmRleDsgbj16LmF2YWlsX2luO2I9dGhpcy5iaXRiO2s9dGhpcy5iaXRrO1xuXHRxPXRoaXMud3JpdGU7bSA9IChxIDwgdGhpcy5yZWFkID8gdGhpcy5yZWFkLXEtMSA6IHRoaXMuZW5kLXEpO1xuXG5cdGlmICh0aGlzLmxhc3Q9PTApe1xuXHQgIHRoaXMubW9kZSA9IElCX1RZUEU7XG5cdCAgYnJlYWs7XG5cdH1cblx0dGhpcy5tb2RlID0gSUJfRFJZO1xuICAgICAgY2FzZSBJQl9EUlk6XG5cdHRoaXMud3JpdGU9cTsgXG5cdHIgPSB0aGlzLmluZmxhdGVfZmx1c2goeiwgcik7IFxuXHRxPXRoaXMud3JpdGU7IG0gPSAocSA8IHRoaXMucmVhZCA/IHRoaXMucmVhZC1xLTEgOiB0aGlzLmVuZC1xKTtcblx0aWYgKHRoaXMucmVhZCAhPSB0aGlzLndyaXRlKXtcblx0ICB0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHQgIHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICB0aGlzLndyaXRlPXE7XG5cdCAgcmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LCByKTtcblx0fVxuXHRtb2RlID0gRE9ORTtcbiAgICAgIGNhc2UgSUJfRE9ORTpcblx0ciA9IFpfU1RSRUFNX0VORDtcblxuXHR0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHR6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdHRoaXMud3JpdGU9cTtcblx0cmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LCByKTtcbiAgICAgIGNhc2UgSUJfQkFEOlxuXHRyID0gWl9EQVRBX0VSUk9SO1xuXG5cdHRoaXMuYml0Yj1iOyB0aGlzLmJpdGs9azsgXG5cdHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0dGhpcy53cml0ZT1xO1xuXHRyZXR1cm4gdGhpcy5pbmZsYXRlX2ZsdXNoKHosIHIpO1xuXG4gICAgICBkZWZhdWx0OlxuXHRyID0gWl9TVFJFQU1fRVJST1I7XG5cblx0dGhpcy5iaXRiPWI7IHRoaXMuYml0az1rOyBcblx0ei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHR0aGlzLndyaXRlPXE7XG5cdHJldHVybiB0aGlzLmluZmxhdGVfZmx1c2goeiwgcik7XG4gICAgICB9XG4gICAgfVxuICB9XG5cbkluZkJsb2Nrcy5wcm90b3R5cGUuZnJlZSA9IGZ1bmN0aW9uKHope1xuICAgIHRoaXMucmVzZXQoeiwgbnVsbCk7XG4gICAgdGhpcy53aW5kb3c9bnVsbDtcbiAgICB0aGlzLmh1ZnRzPW51bGw7XG59XG5cbkluZkJsb2Nrcy5wcm90b3R5cGUuc2V0X2RpY3Rpb25hcnkgPSBmdW5jdGlvbihkLCBzdGFydCwgbil7XG4gICAgYXJyYXlDb3B5KGQsIHN0YXJ0LCB3aW5kb3csIDAsIG4pO1xuICAgIHRoaXMucmVhZCA9IHRoaXMud3JpdGUgPSBuO1xufVxuXG4gIC8vIFJldHVybnMgdHJ1ZSBpZiBpbmZsYXRlIGlzIGN1cnJlbnRseSBhdCB0aGUgZW5kIG9mIGEgYmxvY2sgZ2VuZXJhdGVkXG4gIC8vIGJ5IFpfU1lOQ19GTFVTSCBvciBaX0ZVTExfRkxVU0guIFxuSW5mQmxvY2tzLnByb3RvdHlwZS5zeW5jX3BvaW50ID0gZnVuY3Rpb24oKXtcbiAgICByZXR1cm4gdGhpcy5tb2RlID09IElCX0xFTlM7XG59XG5cbiAgLy8gY29weSBhcyBtdWNoIGFzIHBvc3NpYmxlIGZyb20gdGhlIHNsaWRpbmcgd2luZG93IHRvIHRoZSBvdXRwdXQgYXJlYVxuSW5mQmxvY2tzLnByb3RvdHlwZS5pbmZsYXRlX2ZsdXNoID0gZnVuY3Rpb24oeiwgcil7XG4gICAgdmFyIG47XG4gICAgdmFyIHA7XG4gICAgdmFyIHE7XG5cbiAgICAvLyBsb2NhbCBjb3BpZXMgb2Ygc291cmNlIGFuZCBkZXN0aW5hdGlvbiBwb2ludGVyc1xuICAgIHAgPSB6Lm5leHRfb3V0X2luZGV4O1xuICAgIHEgPSB0aGlzLnJlYWQ7XG5cbiAgICAvLyBjb21wdXRlIG51bWJlciBvZiBieXRlcyB0byBjb3B5IGFzIGZhciBhcyBlbmQgb2Ygd2luZG93XG4gICAgbiA9ICgocSA8PSB0aGlzLndyaXRlID8gdGhpcy53cml0ZSA6IHRoaXMuZW5kKSAtIHEpO1xuICAgIGlmIChuID4gei5hdmFpbF9vdXQpIG4gPSB6LmF2YWlsX291dDtcbiAgICBpZiAobiE9MCAmJiByID09IFpfQlVGX0VSUk9SKSByID0gWl9PSztcblxuICAgIC8vIHVwZGF0ZSBjb3VudGVyc1xuICAgIHouYXZhaWxfb3V0IC09IG47XG4gICAgei50b3RhbF9vdXQgKz0gbjtcblxuICAgIC8vIHVwZGF0ZSBjaGVjayBpbmZvcm1hdGlvblxuICAgIGlmKHRoaXMuY2hlY2tmbiAhPSBudWxsKVxuICAgICAgei5hZGxlcj10aGlzLmNoZWNrPXouX2FkbGVyLmFkbGVyMzIodGhpcy5jaGVjaywgdGhpcy53aW5kb3csIHEsIG4pO1xuXG4gICAgLy8gY29weSBhcyBmYXIgYXMgZW5kIG9mIHdpbmRvd1xuICAgIGFycmF5Q29weSh0aGlzLndpbmRvdywgcSwgei5uZXh0X291dCwgcCwgbik7XG4gICAgcCArPSBuO1xuICAgIHEgKz0gbjtcblxuICAgIC8vIHNlZSBpZiBtb3JlIHRvIGNvcHkgYXQgYmVnaW5uaW5nIG9mIHdpbmRvd1xuICAgIGlmIChxID09IHRoaXMuZW5kKXtcbiAgICAgIC8vIHdyYXAgcG9pbnRlcnNcbiAgICAgIHEgPSAwO1xuICAgICAgaWYgKHRoaXMud3JpdGUgPT0gdGhpcy5lbmQpXG4gICAgICAgIHRoaXMud3JpdGUgPSAwO1xuXG4gICAgICAvLyBjb21wdXRlIGJ5dGVzIHRvIGNvcHlcbiAgICAgIG4gPSB0aGlzLndyaXRlIC0gcTtcbiAgICAgIGlmIChuID4gei5hdmFpbF9vdXQpIG4gPSB6LmF2YWlsX291dDtcbiAgICAgIGlmIChuIT0wICYmIHIgPT0gWl9CVUZfRVJST1IpIHIgPSBaX09LO1xuXG4gICAgICAvLyB1cGRhdGUgY291bnRlcnNcbiAgICAgIHouYXZhaWxfb3V0IC09IG47XG4gICAgICB6LnRvdGFsX291dCArPSBuO1xuXG4gICAgICAvLyB1cGRhdGUgY2hlY2sgaW5mb3JtYXRpb25cbiAgICAgIGlmKHRoaXMuY2hlY2tmbiAhPSBudWxsKVxuXHR6LmFkbGVyPXRoaXMuY2hlY2s9ei5fYWRsZXIuYWRsZXIzMih0aGlzLmNoZWNrLCB0aGlzLndpbmRvdywgcSwgbik7XG5cbiAgICAgIC8vIGNvcHlcbiAgICAgIGFycmF5Q29weSh0aGlzLndpbmRvdywgcSwgei5uZXh0X291dCwgcCwgbik7XG4gICAgICBwICs9IG47XG4gICAgICBxICs9IG47XG4gICAgfVxuXG4gICAgLy8gdXBkYXRlIHBvaW50ZXJzXG4gICAgei5uZXh0X291dF9pbmRleCA9IHA7XG4gICAgdGhpcy5yZWFkID0gcTtcblxuICAgIC8vIGRvbmVcbiAgICByZXR1cm4gcjtcbiAgfVxuXG4vL1xuLy8gSW5mQ29kZXMuamF2YVxuLy9cblxudmFyIElDX1NUQVJUPTA7ICAvLyB4OiBzZXQgdXAgZm9yIExFTlxudmFyIElDX0xFTj0xOyAgICAvLyBpOiBnZXQgbGVuZ3RoL2xpdGVyYWwvZW9iIG5leHRcbnZhciBJQ19MRU5FWFQ9MjsgLy8gaTogZ2V0dGluZyBsZW5ndGggZXh0cmEgKGhhdmUgYmFzZSlcbnZhciBJQ19ESVNUPTM7ICAgLy8gaTogZ2V0IGRpc3RhbmNlIG5leHRcbnZhciBJQ19ESVNURVhUPTQ7Ly8gaTogZ2V0dGluZyBkaXN0YW5jZSBleHRyYVxudmFyIElDX0NPUFk9NTsgICAvLyBvOiBjb3B5aW5nIGJ5dGVzIGluIHdpbmRvdywgd2FpdGluZyBmb3Igc3BhY2VcbnZhciBJQ19MSVQ9NjsgICAgLy8gbzogZ290IGxpdGVyYWwsIHdhaXRpbmcgZm9yIG91dHB1dCBzcGFjZVxudmFyIElDX1dBU0g9NzsgICAvLyBvOiBnb3QgZW9iLCBwb3NzaWJseSBzdGlsbCBvdXRwdXQgd2FpdGluZ1xudmFyIElDX0VORD04OyAgICAvLyB4OiBnb3QgZW9iIGFuZCBhbGwgZGF0YSBmbHVzaGVkXG52YXIgSUNfQkFEQ09ERT05Oy8vIHg6IGdvdCBlcnJvclxuXG5mdW5jdGlvbiBJbmZDb2RlcygpIHtcbn1cblxuSW5mQ29kZXMucHJvdG90eXBlLmluaXQgPSBmdW5jdGlvbihibCwgYmQsIHRsLCB0bF9pbmRleCwgdGQsIHRkX2luZGV4LCB6KSB7XG4gICAgdGhpcy5tb2RlPUlDX1NUQVJUO1xuICAgIHRoaXMubGJpdHM9Ymw7XG4gICAgdGhpcy5kYml0cz1iZDtcbiAgICB0aGlzLmx0cmVlPXRsO1xuICAgIHRoaXMubHRyZWVfaW5kZXg9dGxfaW5kZXg7XG4gICAgdGhpcy5kdHJlZSA9IHRkO1xuICAgIHRoaXMuZHRyZWVfaW5kZXg9dGRfaW5kZXg7XG4gICAgdGhpcy50cmVlPW51bGw7XG59XG5cbkluZkNvZGVzLnByb3RvdHlwZS5wcm9jID0gZnVuY3Rpb24ocywgeiwgcil7IFxuICAgIHZhciBqOyAgICAgICAgICAgICAgLy8gdGVtcG9yYXJ5IHN0b3JhZ2VcbiAgICB2YXIgdDsgICAgICAgICAgICAgIC8vIHRlbXBvcmFyeSBwb2ludGVyIChpbnRbXSlcbiAgICB2YXIgdGluZGV4OyAgICAgICAgIC8vIHRlbXBvcmFyeSBwb2ludGVyXG4gICAgdmFyIGU7ICAgICAgICAgICAgICAvLyBleHRyYSBiaXRzIG9yIG9wZXJhdGlvblxuICAgIHZhciBiPTA7ICAgICAgICAgICAgLy8gYml0IGJ1ZmZlclxuICAgIHZhciBrPTA7ICAgICAgICAgICAgLy8gYml0cyBpbiBiaXQgYnVmZmVyXG4gICAgdmFyIHA9MDsgICAgICAgICAgICAvLyBpbnB1dCBkYXRhIHBvaW50ZXJcbiAgICB2YXIgbjsgICAgICAgICAgICAgIC8vIGJ5dGVzIGF2YWlsYWJsZSB0aGVyZVxuICAgIHZhciBxOyAgICAgICAgICAgICAgLy8gb3V0cHV0IHdpbmRvdyB3cml0ZSBwb2ludGVyXG4gICAgdmFyIG07ICAgICAgICAgICAgICAvLyBieXRlcyB0byBlbmQgb2Ygd2luZG93IG9yIHJlYWQgcG9pbnRlclxuICAgIHZhciBmOyAgICAgICAgICAgICAgLy8gcG9pbnRlciB0byBjb3B5IHN0cmluZ3MgZnJvbVxuXG4gICAgLy8gY29weSBpbnB1dC9vdXRwdXQgaW5mb3JtYXRpb24gdG8gbG9jYWxzIChVUERBVEUgbWFjcm8gcmVzdG9yZXMpXG4gICAgcD16Lm5leHRfaW5faW5kZXg7bj16LmF2YWlsX2luO2I9cy5iaXRiO2s9cy5iaXRrO1xuICAgIHE9cy53cml0ZTttPXE8cy5yZWFkP3MucmVhZC1xLTE6cy5lbmQtcTtcblxuICAgIC8vIHByb2Nlc3MgaW5wdXQgYW5kIG91dHB1dCBiYXNlZCBvbiBjdXJyZW50IHN0YXRlXG4gICAgd2hpbGUgKHRydWUpe1xuICAgICAgc3dpdGNoICh0aGlzLm1vZGUpe1xuXHQvLyB3YWl0aW5nIGZvciBcImk6XCI9aW5wdXQsIFwibzpcIj1vdXRwdXQsIFwieDpcIj1ub3RoaW5nXG4gICAgICBjYXNlIElDX1NUQVJUOiAgICAgICAgIC8vIHg6IHNldCB1cCBmb3IgTEVOXG5cdGlmIChtID49IDI1OCAmJiBuID49IDEwKXtcblxuXHQgIHMuYml0Yj1iO3MuYml0az1rO1xuXHQgIHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICBzLndyaXRlPXE7XG5cdCAgciA9IHRoaXMuaW5mbGF0ZV9mYXN0KHRoaXMubGJpdHMsIHRoaXMuZGJpdHMsIFxuXHRcdFx0ICAgdGhpcy5sdHJlZSwgdGhpcy5sdHJlZV9pbmRleCwgXG5cdFx0XHQgICB0aGlzLmR0cmVlLCB0aGlzLmR0cmVlX2luZGV4LFxuXHRcdFx0ICAgcywgeik7XG5cblx0ICBwPXoubmV4dF9pbl9pbmRleDtuPXouYXZhaWxfaW47Yj1zLmJpdGI7az1zLmJpdGs7XG5cdCAgcT1zLndyaXRlO209cTxzLnJlYWQ/cy5yZWFkLXEtMTpzLmVuZC1xO1xuXG5cdCAgaWYgKHIgIT0gWl9PSyl7XG5cdCAgICB0aGlzLm1vZGUgPSByID09IFpfU1RSRUFNX0VORCA/IElDX1dBU0ggOiBJQ19CQURDT0RFO1xuXHQgICAgYnJlYWs7XG5cdCAgfVxuXHR9XG5cdHRoaXMubmVlZCA9IHRoaXMubGJpdHM7XG5cdHRoaXMudHJlZSA9IHRoaXMubHRyZWU7XG5cdHRoaXMudHJlZV9pbmRleD10aGlzLmx0cmVlX2luZGV4O1xuXG5cdHRoaXMubW9kZSA9IElDX0xFTjtcbiAgICAgIGNhc2UgSUNfTEVOOiAgICAgICAgICAgLy8gaTogZ2V0IGxlbmd0aC9saXRlcmFsL2VvYiBuZXh0XG5cdGogPSB0aGlzLm5lZWQ7XG5cblx0d2hpbGUoazwoaikpe1xuXHQgIGlmKG4hPTApcj1aX09LO1xuXHQgIGVsc2V7XG5cblx0ICAgIHMuYml0Yj1iO3MuYml0az1rO1xuXHQgICAgei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgICAgcy53cml0ZT1xO1xuXHQgICAgcmV0dXJuIHMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgIH1cblx0ICBuLS07XG5cdCAgYnw9KHoubmV4dF9pbltwKytdJjB4ZmYpPDxrO1xuXHQgIGsrPTg7XG5cdH1cblxuXHR0aW5kZXg9KHRoaXMudHJlZV9pbmRleCsoYiZpbmZsYXRlX21hc2tbal0pKSozO1xuXG5cdGI+Pj49KHRoaXMudHJlZVt0aW5kZXgrMV0pO1xuXHRrLT0odGhpcy50cmVlW3RpbmRleCsxXSk7XG5cblx0ZT10aGlzLnRyZWVbdGluZGV4XTtcblxuXHRpZihlID09IDApeyAgICAgICAgICAgICAgIC8vIGxpdGVyYWxcblx0ICB0aGlzLmxpdCA9IHRoaXMudHJlZVt0aW5kZXgrMl07XG5cdCAgdGhpcy5tb2RlID0gSUNfTElUO1xuXHQgIGJyZWFrO1xuXHR9XG5cdGlmKChlICYgMTYpIT0wICl7ICAgICAgICAgIC8vIGxlbmd0aFxuXHQgIHRoaXMuZ2V0ID0gZSAmIDE1O1xuXHQgIHRoaXMubGVuID0gdGhpcy50cmVlW3RpbmRleCsyXTtcblx0ICB0aGlzLm1vZGUgPSBJQ19MRU5FWFQ7XG5cdCAgYnJlYWs7XG5cdH1cblx0aWYgKChlICYgNjQpID09IDApeyAgICAgICAgLy8gbmV4dCB0YWJsZVxuXHQgIHRoaXMubmVlZCA9IGU7XG5cdCAgdGhpcy50cmVlX2luZGV4ID0gdGluZGV4LzMgKyB0aGlzLnRyZWVbdGluZGV4KzJdO1xuXHQgIGJyZWFrO1xuXHR9XG5cdGlmICgoZSAmIDMyKSE9MCl7ICAgICAgICAgICAgICAgLy8gZW5kIG9mIGJsb2NrXG5cdCAgdGhpcy5tb2RlID0gSUNfV0FTSDtcblx0ICBicmVhaztcblx0fVxuXHR0aGlzLm1vZGUgPSBJQ19CQURDT0RFOyAgICAgICAgLy8gaW52YWxpZCBjb2RlXG5cdHoubXNnID0gXCJpbnZhbGlkIGxpdGVyYWwvbGVuZ3RoIGNvZGVcIjtcblx0ciA9IFpfREFUQV9FUlJPUjtcblxuXHRzLmJpdGI9YjtzLmJpdGs9aztcblx0ei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHRzLndyaXRlPXE7XG5cdHJldHVybiBzLmluZmxhdGVfZmx1c2goeixyKTtcblxuICAgICAgY2FzZSBJQ19MRU5FWFQ6ICAgICAgICAvLyBpOiBnZXR0aW5nIGxlbmd0aCBleHRyYSAoaGF2ZSBiYXNlKVxuXHRqID0gdGhpcy5nZXQ7XG5cblx0d2hpbGUoazwoaikpe1xuXHQgIGlmKG4hPTApcj1aX09LO1xuXHQgIGVsc2V7XG5cblx0ICAgIHMuYml0Yj1iO3MuYml0az1rO1xuXHQgICAgei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgICAgcy53cml0ZT1xO1xuXHQgICAgcmV0dXJuIHMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgIH1cblx0ICBuLS07IGJ8PSh6Lm5leHRfaW5bcCsrXSYweGZmKTw8aztcblx0ICBrKz04O1xuXHR9XG5cblx0dGhpcy5sZW4gKz0gKGIgJiBpbmZsYXRlX21hc2tbal0pO1xuXG5cdGI+Pj1qO1xuXHRrLT1qO1xuXG5cdHRoaXMubmVlZCA9IHRoaXMuZGJpdHM7XG5cdHRoaXMudHJlZSA9IHRoaXMuZHRyZWU7XG5cdHRoaXMudHJlZV9pbmRleCA9IHRoaXMuZHRyZWVfaW5kZXg7XG5cdHRoaXMubW9kZSA9IElDX0RJU1Q7XG4gICAgICBjYXNlIElDX0RJU1Q6ICAgICAgICAgIC8vIGk6IGdldCBkaXN0YW5jZSBuZXh0XG5cdGogPSB0aGlzLm5lZWQ7XG5cblx0d2hpbGUoazwoaikpe1xuXHQgIGlmKG4hPTApcj1aX09LO1xuXHQgIGVsc2V7XG5cblx0ICAgIHMuYml0Yj1iO3MuYml0az1rO1xuXHQgICAgei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgICAgcy53cml0ZT1xO1xuXHQgICAgcmV0dXJuIHMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgIH1cblx0ICBuLS07IGJ8PSh6Lm5leHRfaW5bcCsrXSYweGZmKTw8aztcblx0ICBrKz04O1xuXHR9XG5cblx0dGluZGV4PSh0aGlzLnRyZWVfaW5kZXgrKGIgJiBpbmZsYXRlX21hc2tbal0pKSozO1xuXG5cdGI+Pj10aGlzLnRyZWVbdGluZGV4KzFdO1xuXHRrLT10aGlzLnRyZWVbdGluZGV4KzFdO1xuXG5cdGUgPSAodGhpcy50cmVlW3RpbmRleF0pO1xuXHRpZigoZSAmIDE2KSE9MCl7ICAgICAgICAgICAgICAgLy8gZGlzdGFuY2Vcblx0ICB0aGlzLmdldCA9IGUgJiAxNTtcblx0ICB0aGlzLmRpc3QgPSB0aGlzLnRyZWVbdGluZGV4KzJdO1xuXHQgIHRoaXMubW9kZSA9IElDX0RJU1RFWFQ7XG5cdCAgYnJlYWs7XG5cdH1cblx0aWYgKChlICYgNjQpID09IDApeyAgICAgICAgLy8gbmV4dCB0YWJsZVxuXHQgIHRoaXMubmVlZCA9IGU7XG5cdCAgdGhpcy50cmVlX2luZGV4ID0gdGluZGV4LzMgKyB0aGlzLnRyZWVbdGluZGV4KzJdO1xuXHQgIGJyZWFrO1xuXHR9XG5cdHRoaXMubW9kZSA9IElDX0JBRENPREU7ICAgICAgICAvLyBpbnZhbGlkIGNvZGVcblx0ei5tc2cgPSBcImludmFsaWQgZGlzdGFuY2UgY29kZVwiO1xuXHRyID0gWl9EQVRBX0VSUk9SO1xuXG5cdHMuYml0Yj1iO3MuYml0az1rO1xuXHR6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdHMud3JpdGU9cTtcblx0cmV0dXJuIHMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXG4gICAgICBjYXNlIElDX0RJU1RFWFQ6ICAgICAgIC8vIGk6IGdldHRpbmcgZGlzdGFuY2UgZXh0cmFcblx0aiA9IHRoaXMuZ2V0O1xuXG5cdHdoaWxlKGs8KGopKXtcblx0ICBpZihuIT0wKXI9Wl9PSztcblx0ICBlbHNle1xuXG5cdCAgICBzLmJpdGI9YjtzLmJpdGs9aztcblx0ICAgIHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICAgIHMud3JpdGU9cTtcblx0ICAgIHJldHVybiBzLmluZmxhdGVfZmx1c2goeixyKTtcblx0ICB9XG5cdCAgbi0tOyBifD0oei5uZXh0X2luW3ArK10mMHhmZik8PGs7XG5cdCAgays9ODtcblx0fVxuXG5cdHRoaXMuZGlzdCArPSAoYiAmIGluZmxhdGVfbWFza1tqXSk7XG5cblx0Yj4+PWo7XG5cdGstPWo7XG5cblx0dGhpcy5tb2RlID0gSUNfQ09QWTtcbiAgICAgIGNhc2UgSUNfQ09QWTogICAgICAgICAgLy8gbzogY29weWluZyBieXRlcyBpbiB3aW5kb3csIHdhaXRpbmcgZm9yIHNwYWNlXG4gICAgICAgIGYgPSBxIC0gdGhpcy5kaXN0O1xuICAgICAgICB3aGlsZShmIDwgMCl7ICAgICAvLyBtb2R1bG8gd2luZG93IHNpemUtXCJ3aGlsZVwiIGluc3RlYWRcbiAgICAgICAgICBmICs9IHMuZW5kOyAgICAgLy8gb2YgXCJpZlwiIGhhbmRsZXMgaW52YWxpZCBkaXN0YW5jZXNcblx0fVxuXHR3aGlsZSAodGhpcy5sZW4hPTApe1xuXG5cdCAgaWYobT09MCl7XG5cdCAgICBpZihxPT1zLmVuZCYmcy5yZWFkIT0wKXtxPTA7bT1xPHMucmVhZD9zLnJlYWQtcS0xOnMuZW5kLXE7fVxuXHQgICAgaWYobT09MCl7XG5cdCAgICAgIHMud3JpdGU9cTsgcj1zLmluZmxhdGVfZmx1c2goeixyKTtcblx0ICAgICAgcT1zLndyaXRlO209cTxzLnJlYWQ/cy5yZWFkLXEtMTpzLmVuZC1xO1xuXG5cdCAgICAgIGlmKHE9PXMuZW5kJiZzLnJlYWQhPTApe3E9MDttPXE8cy5yZWFkP3MucmVhZC1xLTE6cy5lbmQtcTt9XG5cblx0ICAgICAgaWYobT09MCl7XG5cdFx0cy5iaXRiPWI7cy5iaXRrPWs7XG5cdFx0ei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHRcdHMud3JpdGU9cTtcblx0XHRyZXR1cm4gcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdCAgICAgIH0gIFxuXHQgICAgfVxuXHQgIH1cblxuXHQgIHMud2luZG93W3ErK109cy53aW5kb3dbZisrXTsgbS0tO1xuXG5cdCAgaWYgKGYgPT0gcy5lbmQpXG4gICAgICAgICAgICBmID0gMDtcblx0ICB0aGlzLmxlbi0tO1xuXHR9XG5cdHRoaXMubW9kZSA9IElDX1NUQVJUO1xuXHRicmVhaztcbiAgICAgIGNhc2UgSUNfTElUOiAgICAgICAgICAgLy8gbzogZ290IGxpdGVyYWwsIHdhaXRpbmcgZm9yIG91dHB1dCBzcGFjZVxuXHRpZihtPT0wKXtcblx0ICBpZihxPT1zLmVuZCYmcy5yZWFkIT0wKXtxPTA7bT1xPHMucmVhZD9zLnJlYWQtcS0xOnMuZW5kLXE7fVxuXHQgIGlmKG09PTApe1xuXHQgICAgcy53cml0ZT1xOyByPXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgICAgcT1zLndyaXRlO209cTxzLnJlYWQ/cy5yZWFkLXEtMTpzLmVuZC1xO1xuXG5cdCAgICBpZihxPT1zLmVuZCYmcy5yZWFkIT0wKXtxPTA7bT1xPHMucmVhZD9zLnJlYWQtcS0xOnMuZW5kLXE7fVxuXHQgICAgaWYobT09MCl7XG5cdCAgICAgIHMuYml0Yj1iO3MuYml0az1rO1xuXHQgICAgICB6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgICAgIHMud3JpdGU9cTtcblx0ICAgICAgcmV0dXJuIHMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgICAgfVxuXHQgIH1cblx0fVxuXHRyPVpfT0s7XG5cblx0cy53aW5kb3dbcSsrXT10aGlzLmxpdDsgbS0tO1xuXG5cdHRoaXMubW9kZSA9IElDX1NUQVJUO1xuXHRicmVhaztcbiAgICAgIGNhc2UgSUNfV0FTSDogICAgICAgICAgIC8vIG86IGdvdCBlb2IsIHBvc3NpYmx5IG1vcmUgb3V0cHV0XG5cdGlmIChrID4gNyl7ICAgICAgICAvLyByZXR1cm4gdW51c2VkIGJ5dGUsIGlmIGFueVxuXHQgIGsgLT0gODtcblx0ICBuKys7XG5cdCAgcC0tOyAgICAgICAgICAgICAvLyBjYW4gYWx3YXlzIHJldHVybiBvbmVcblx0fVxuXG5cdHMud3JpdGU9cTsgcj1zLmluZmxhdGVfZmx1c2goeixyKTtcblx0cT1zLndyaXRlO209cTxzLnJlYWQ/cy5yZWFkLXEtMTpzLmVuZC1xO1xuXG5cdGlmIChzLnJlYWQgIT0gcy53cml0ZSl7XG5cdCAgcy5iaXRiPWI7cy5iaXRrPWs7XG5cdCAgei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgIHMud3JpdGU9cTtcblx0ICByZXR1cm4gcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdH1cblx0dGhpcy5tb2RlID0gSUNfRU5EO1xuICAgICAgY2FzZSBJQ19FTkQ6XG5cdHIgPSBaX1NUUkVBTV9FTkQ7XG5cdHMuYml0Yj1iO3MuYml0az1rO1xuXHR6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdHMud3JpdGU9cTtcblx0cmV0dXJuIHMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXG4gICAgICBjYXNlIElDX0JBRENPREU6ICAgICAgIC8vIHg6IGdvdCBlcnJvclxuXG5cdHIgPSBaX0RBVEFfRVJST1I7XG5cblx0cy5iaXRiPWI7cy5iaXRrPWs7XG5cdHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0cy53cml0ZT1xO1xuXHRyZXR1cm4gcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cbiAgICAgIGRlZmF1bHQ6XG5cdHIgPSBaX1NUUkVBTV9FUlJPUjtcblxuXHRzLmJpdGI9YjtzLmJpdGs9aztcblx0ei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHRzLndyaXRlPXE7XG5cdHJldHVybiBzLmluZmxhdGVfZmx1c2goeixyKTtcbiAgICAgIH1cbiAgICB9XG4gIH1cblxuSW5mQ29kZXMucHJvdG90eXBlLmZyZWUgPSBmdW5jdGlvbih6KXtcbiAgICAvLyAgWkZSRUUoeiwgYyk7XG59XG5cbiAgLy8gQ2FsbGVkIHdpdGggbnVtYmVyIG9mIGJ5dGVzIGxlZnQgdG8gd3JpdGUgaW4gd2luZG93IGF0IGxlYXN0IDI1OFxuICAvLyAodGhlIG1heGltdW0gc3RyaW5nIGxlbmd0aCkgYW5kIG51bWJlciBvZiBpbnB1dCBieXRlcyBhdmFpbGFibGVcbiAgLy8gYXQgbGVhc3QgdGVuLiAgVGhlIHRlbiBieXRlcyBhcmUgc2l4IGJ5dGVzIGZvciB0aGUgbG9uZ2VzdCBsZW5ndGgvXG4gIC8vIGRpc3RhbmNlIHBhaXIgcGx1cyBmb3VyIGJ5dGVzIGZvciBvdmVybG9hZGluZyB0aGUgYml0IGJ1ZmZlci5cblxuSW5mQ29kZXMucHJvdG90eXBlLmluZmxhdGVfZmFzdCA9IGZ1bmN0aW9uKGJsLCBiZCwgdGwsIHRsX2luZGV4LCB0ZCwgdGRfaW5kZXgsIHMsIHopIHtcbiAgICB2YXIgdDsgICAgICAgICAgICAgICAgLy8gdGVtcG9yYXJ5IHBvaW50ZXJcbiAgICB2YXIgICB0cDsgICAgICAgICAgICAgLy8gdGVtcG9yYXJ5IHBvaW50ZXIgKGludFtdKVxuICAgIHZhciB0cF9pbmRleDsgICAgICAgICAvLyB0ZW1wb3JhcnkgcG9pbnRlclxuICAgIHZhciBlOyAgICAgICAgICAgICAgICAvLyBleHRyYSBiaXRzIG9yIG9wZXJhdGlvblxuICAgIHZhciBiOyAgICAgICAgICAgICAgICAvLyBiaXQgYnVmZmVyXG4gICAgdmFyIGs7ICAgICAgICAgICAgICAgIC8vIGJpdHMgaW4gYml0IGJ1ZmZlclxuICAgIHZhciBwOyAgICAgICAgICAgICAgICAvLyBpbnB1dCBkYXRhIHBvaW50ZXJcbiAgICB2YXIgbjsgICAgICAgICAgICAgICAgLy8gYnl0ZXMgYXZhaWxhYmxlIHRoZXJlXG4gICAgdmFyIHE7ICAgICAgICAgICAgICAgIC8vIG91dHB1dCB3aW5kb3cgd3JpdGUgcG9pbnRlclxuICAgIHZhciBtOyAgICAgICAgICAgICAgICAvLyBieXRlcyB0byBlbmQgb2Ygd2luZG93IG9yIHJlYWQgcG9pbnRlclxuICAgIHZhciBtbDsgICAgICAgICAgICAgICAvLyBtYXNrIGZvciBsaXRlcmFsL2xlbmd0aCB0cmVlXG4gICAgdmFyIG1kOyAgICAgICAgICAgICAgIC8vIG1hc2sgZm9yIGRpc3RhbmNlIHRyZWVcbiAgICB2YXIgYzsgICAgICAgICAgICAgICAgLy8gYnl0ZXMgdG8gY29weVxuICAgIHZhciBkOyAgICAgICAgICAgICAgICAvLyBkaXN0YW5jZSBiYWNrIHRvIGNvcHkgZnJvbVxuICAgIHZhciByOyAgICAgICAgICAgICAgICAvLyBjb3B5IHNvdXJjZSBwb2ludGVyXG5cbiAgICB2YXIgdHBfaW5kZXhfdF8zOyAgICAgLy8gKHRwX2luZGV4K3QpKjNcblxuICAgIC8vIGxvYWQgaW5wdXQsIG91dHB1dCwgYml0IHZhbHVlc1xuICAgIHA9ei5uZXh0X2luX2luZGV4O249ei5hdmFpbF9pbjtiPXMuYml0YjtrPXMuYml0aztcbiAgICBxPXMud3JpdGU7bT1xPHMucmVhZD9zLnJlYWQtcS0xOnMuZW5kLXE7XG5cbiAgICAvLyBpbml0aWFsaXplIG1hc2tzXG4gICAgbWwgPSBpbmZsYXRlX21hc2tbYmxdO1xuICAgIG1kID0gaW5mbGF0ZV9tYXNrW2JkXTtcblxuICAgIC8vIGRvIHVudGlsIG5vdCBlbm91Z2ggaW5wdXQgb3Igb3V0cHV0IHNwYWNlIGZvciBmYXN0IGxvb3BcbiAgICBkbyB7ICAgICAgICAgICAgICAgICAgICAgICAgICAvLyBhc3N1bWUgY2FsbGVkIHdpdGggbSA+PSAyNTggJiYgbiA+PSAxMFxuICAgICAgLy8gZ2V0IGxpdGVyYWwvbGVuZ3RoIGNvZGVcbiAgICAgIHdoaWxlKGs8KDIwKSl7ICAgICAgICAgICAgICAvLyBtYXggYml0cyBmb3IgbGl0ZXJhbC9sZW5ndGggY29kZVxuXHRuLS07XG5cdGJ8PSh6Lm5leHRfaW5bcCsrXSYweGZmKTw8aztrKz04O1xuICAgICAgfVxuXG4gICAgICB0PSBiJm1sO1xuICAgICAgdHA9dGw7IFxuICAgICAgdHBfaW5kZXg9dGxfaW5kZXg7XG4gICAgICB0cF9pbmRleF90XzM9KHRwX2luZGV4K3QpKjM7XG4gICAgICBpZiAoKGUgPSB0cFt0cF9pbmRleF90XzNdKSA9PSAwKXtcblx0Yj4+PSh0cFt0cF9pbmRleF90XzMrMV0pOyBrLT0odHBbdHBfaW5kZXhfdF8zKzFdKTtcblxuXHRzLndpbmRvd1txKytdID0gdHBbdHBfaW5kZXhfdF8zKzJdO1xuXHRtLS07XG5cdGNvbnRpbnVlO1xuICAgICAgfVxuICAgICAgZG8ge1xuXG5cdGI+Pj0odHBbdHBfaW5kZXhfdF8zKzFdKTsgay09KHRwW3RwX2luZGV4X3RfMysxXSk7XG5cblx0aWYoKGUmMTYpIT0wKXtcblx0ICBlICY9IDE1O1xuXHQgIGMgPSB0cFt0cF9pbmRleF90XzMrMl0gKyAoYiAmIGluZmxhdGVfbWFza1tlXSk7XG5cblx0ICBiPj49ZTsgay09ZTtcblxuXHQgIC8vIGRlY29kZSBkaXN0YW5jZSBiYXNlIG9mIGJsb2NrIHRvIGNvcHlcblx0ICB3aGlsZShrPCgxNSkpeyAgICAgICAgICAgLy8gbWF4IGJpdHMgZm9yIGRpc3RhbmNlIGNvZGVcblx0ICAgIG4tLTtcblx0ICAgIGJ8PSh6Lm5leHRfaW5bcCsrXSYweGZmKTw8aztrKz04O1xuXHQgIH1cblxuXHQgIHQ9IGImbWQ7XG5cdCAgdHA9dGQ7XG5cdCAgdHBfaW5kZXg9dGRfaW5kZXg7XG4gICAgICAgICAgdHBfaW5kZXhfdF8zPSh0cF9pbmRleCt0KSozO1xuXHQgIGUgPSB0cFt0cF9pbmRleF90XzNdO1xuXG5cdCAgZG8ge1xuXG5cdCAgICBiPj49KHRwW3RwX2luZGV4X3RfMysxXSk7IGstPSh0cFt0cF9pbmRleF90XzMrMV0pO1xuXG5cdCAgICBpZigoZSYxNikhPTApe1xuXHQgICAgICAvLyBnZXQgZXh0cmEgYml0cyB0byBhZGQgdG8gZGlzdGFuY2UgYmFzZVxuXHQgICAgICBlICY9IDE1O1xuXHQgICAgICB3aGlsZShrPChlKSl7ICAgICAgICAgLy8gZ2V0IGV4dHJhIGJpdHMgKHVwIHRvIDEzKVxuXHRcdG4tLTtcblx0XHRifD0oei5uZXh0X2luW3ArK10mMHhmZik8PGs7ays9ODtcblx0ICAgICAgfVxuXG5cdCAgICAgIGQgPSB0cFt0cF9pbmRleF90XzMrMl0gKyAoYiZpbmZsYXRlX21hc2tbZV0pO1xuXG5cdCAgICAgIGI+Pj0oZSk7IGstPShlKTtcblxuXHQgICAgICAvLyBkbyB0aGUgY29weVxuXHQgICAgICBtIC09IGM7XG5cdCAgICAgIGlmIChxID49IGQpeyAgICAgICAgICAgICAgICAvLyBvZmZzZXQgYmVmb3JlIGRlc3Rcblx0XHQvLyAganVzdCBjb3B5XG5cdFx0cj1xLWQ7XG5cdFx0aWYocS1yPjAgJiYgMj4ocS1yKSl7ICAgICAgICAgICBcblx0XHQgIHMud2luZG93W3ErK109cy53aW5kb3dbcisrXTsgLy8gbWluaW11bSBjb3VudCBpcyB0aHJlZSxcblx0XHQgIHMud2luZG93W3ErK109cy53aW5kb3dbcisrXTsgLy8gc28gdW5yb2xsIGxvb3AgYSBsaXR0bGVcblx0XHQgIGMtPTI7XG5cdFx0fVxuXHRcdGVsc2V7XG5cdFx0ICBzLndpbmRvd1txKytdPXMud2luZG93W3IrK107IC8vIG1pbmltdW0gY291bnQgaXMgdGhyZWUsXG5cdFx0ICBzLndpbmRvd1txKytdPXMud2luZG93W3IrK107IC8vIHNvIHVucm9sbCBsb29wIGEgbGl0dGxlXG5cdFx0ICBjLT0yO1xuXHRcdH1cblx0ICAgICAgfVxuXHQgICAgICBlbHNleyAgICAgICAgICAgICAgICAgIC8vIGVsc2Ugb2Zmc2V0IGFmdGVyIGRlc3RpbmF0aW9uXG4gICAgICAgICAgICAgICAgcj1xLWQ7XG4gICAgICAgICAgICAgICAgZG97XG4gICAgICAgICAgICAgICAgICByKz1zLmVuZDsgICAgICAgICAgLy8gZm9yY2UgcG9pbnRlciBpbiB3aW5kb3dcbiAgICAgICAgICAgICAgICB9d2hpbGUocjwwKTsgICAgICAgICAvLyBjb3ZlcnMgaW52YWxpZCBkaXN0YW5jZXNcblx0XHRlPXMuZW5kLXI7XG5cdFx0aWYoYz5lKXsgICAgICAgICAgICAgLy8gaWYgc291cmNlIGNyb3NzZXMsXG5cdFx0ICBjLT1lOyAgICAgICAgICAgICAgLy8gd3JhcHBlZCBjb3B5XG5cdFx0ICBpZihxLXI+MCAmJiBlPihxLXIpKXsgICAgICAgICAgIFxuXHRcdCAgICBkb3tzLndpbmRvd1txKytdID0gcy53aW5kb3dbcisrXTt9XG5cdFx0ICAgIHdoaWxlKC0tZSE9MCk7XG5cdFx0ICB9XG5cdFx0ICBlbHNle1xuXHRcdCAgICBhcnJheUNvcHkocy53aW5kb3csIHIsIHMud2luZG93LCBxLCBlKTtcblx0XHQgICAgcSs9ZTsgcis9ZTsgZT0wO1xuXHRcdCAgfVxuXHRcdCAgciA9IDA7ICAgICAgICAgICAgICAgICAgLy8gY29weSByZXN0IGZyb20gc3RhcnQgb2Ygd2luZG93XG5cdFx0fVxuXG5cdCAgICAgIH1cblxuXHQgICAgICAvLyBjb3B5IGFsbCBvciB3aGF0J3MgbGVmdFxuICAgICAgICAgICAgICBkb3tzLndpbmRvd1txKytdID0gcy53aW5kb3dbcisrXTt9XG5cdFx0d2hpbGUoLS1jIT0wKTtcblx0ICAgICAgYnJlYWs7XG5cdCAgICB9XG5cdCAgICBlbHNlIGlmKChlJjY0KT09MCl7XG5cdCAgICAgIHQrPXRwW3RwX2luZGV4X3RfMysyXTtcblx0ICAgICAgdCs9KGImaW5mbGF0ZV9tYXNrW2VdKTtcblx0ICAgICAgdHBfaW5kZXhfdF8zPSh0cF9pbmRleCt0KSozO1xuXHQgICAgICBlPXRwW3RwX2luZGV4X3RfM107XG5cdCAgICB9XG5cdCAgICBlbHNle1xuXHQgICAgICB6Lm1zZyA9IFwiaW52YWxpZCBkaXN0YW5jZSBjb2RlXCI7XG5cblx0ICAgICAgYz16LmF2YWlsX2luLW47Yz0oaz4+Myk8Yz9rPj4zOmM7bis9YztwLT1jO2stPWM8PDM7XG5cblx0ICAgICAgcy5iaXRiPWI7cy5iaXRrPWs7XG5cdCAgICAgIHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICAgICAgcy53cml0ZT1xO1xuXG5cdCAgICAgIHJldHVybiBaX0RBVEFfRVJST1I7XG5cdCAgICB9XG5cdCAgfVxuXHQgIHdoaWxlKHRydWUpO1xuXHQgIGJyZWFrO1xuXHR9XG5cblx0aWYoKGUmNjQpPT0wKXtcblx0ICB0Kz10cFt0cF9pbmRleF90XzMrMl07XG5cdCAgdCs9KGImaW5mbGF0ZV9tYXNrW2VdKTtcblx0ICB0cF9pbmRleF90XzM9KHRwX2luZGV4K3QpKjM7XG5cdCAgaWYoKGU9dHBbdHBfaW5kZXhfdF8zXSk9PTApe1xuXG5cdCAgICBiPj49KHRwW3RwX2luZGV4X3RfMysxXSk7IGstPSh0cFt0cF9pbmRleF90XzMrMV0pO1xuXG5cdCAgICBzLndpbmRvd1txKytdPXRwW3RwX2luZGV4X3RfMysyXTtcblx0ICAgIG0tLTtcblx0ICAgIGJyZWFrO1xuXHQgIH1cblx0fVxuXHRlbHNlIGlmKChlJjMyKSE9MCl7XG5cblx0ICBjPXouYXZhaWxfaW4tbjtjPShrPj4zKTxjP2s+PjM6YztuKz1jO3AtPWM7ay09Yzw8MztcbiBcblx0ICBzLmJpdGI9YjtzLmJpdGs9aztcblx0ICB6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgcy53cml0ZT1xO1xuXG5cdCAgcmV0dXJuIFpfU1RSRUFNX0VORDtcblx0fVxuXHRlbHNle1xuXHQgIHoubXNnPVwiaW52YWxpZCBsaXRlcmFsL2xlbmd0aCBjb2RlXCI7XG5cblx0ICBjPXouYXZhaWxfaW4tbjtjPShrPj4zKTxjP2s+PjM6YztuKz1jO3AtPWM7ay09Yzw8MztcblxuXHQgIHMuYml0Yj1iO3MuYml0az1rO1xuXHQgIHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICBzLndyaXRlPXE7XG5cblx0ICByZXR1cm4gWl9EQVRBX0VSUk9SO1xuXHR9XG4gICAgICB9IFxuICAgICAgd2hpbGUodHJ1ZSk7XG4gICAgfSBcbiAgICB3aGlsZShtPj0yNTggJiYgbj49IDEwKTtcblxuICAgIC8vIG5vdCBlbm91Z2ggaW5wdXQgb3Igb3V0cHV0LS1yZXN0b3JlIHBvaW50ZXJzIGFuZCByZXR1cm5cbiAgICBjPXouYXZhaWxfaW4tbjtjPShrPj4zKTxjP2s+PjM6YztuKz1jO3AtPWM7ay09Yzw8MztcblxuICAgIHMuYml0Yj1iO3MuYml0az1rO1xuICAgIHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcbiAgICBzLndyaXRlPXE7XG5cbiAgICByZXR1cm4gWl9PSztcbn1cblxuLy9cbi8vIEluZlRyZWUuamF2YVxuLy9cblxuZnVuY3Rpb24gSW5mVHJlZSgpIHtcbn1cblxuSW5mVHJlZS5wcm90b3R5cGUuaHVmdF9idWlsZCA9IGZ1bmN0aW9uKGIsIGJpbmRleCwgbiwgcywgZCwgZSwgdCwgbSwgaHAsIGhuLCB2KSB7XG5cbiAgICAvLyBHaXZlbiBhIGxpc3Qgb2YgY29kZSBsZW5ndGhzIGFuZCBhIG1heGltdW0gdGFibGUgc2l6ZSwgbWFrZSBhIHNldCBvZlxuICAgIC8vIHRhYmxlcyB0byBkZWNvZGUgdGhhdCBzZXQgb2YgY29kZXMuICBSZXR1cm4gWl9PSyBvbiBzdWNjZXNzLCBaX0JVRl9FUlJPUlxuICAgIC8vIGlmIHRoZSBnaXZlbiBjb2RlIHNldCBpcyBpbmNvbXBsZXRlICh0aGUgdGFibGVzIGFyZSBzdGlsbCBidWlsdCBpbiB0aGlzXG4gICAgLy8gY2FzZSksIFpfREFUQV9FUlJPUiBpZiB0aGUgaW5wdXQgaXMgaW52YWxpZCAoYW4gb3Zlci1zdWJzY3JpYmVkIHNldCBvZlxuICAgIC8vIGxlbmd0aHMpLCBvciBaX01FTV9FUlJPUiBpZiBub3QgZW5vdWdoIG1lbW9yeS5cblxuICAgIHZhciBhOyAgICAgICAgICAgICAgICAgICAgICAgLy8gY291bnRlciBmb3IgY29kZXMgb2YgbGVuZ3RoIGtcbiAgICB2YXIgZjsgICAgICAgICAgICAgICAgICAgICAgIC8vIGkgcmVwZWF0cyBpbiB0YWJsZSBldmVyeSBmIGVudHJpZXNcbiAgICB2YXIgZzsgICAgICAgICAgICAgICAgICAgICAgIC8vIG1heGltdW0gY29kZSBsZW5ndGhcbiAgICB2YXIgaDsgICAgICAgICAgICAgICAgICAgICAgIC8vIHRhYmxlIGxldmVsXG4gICAgdmFyIGk7ICAgICAgICAgICAgICAgICAgICAgICAvLyBjb3VudGVyLCBjdXJyZW50IGNvZGVcbiAgICB2YXIgajsgICAgICAgICAgICAgICAgICAgICAgIC8vIGNvdW50ZXJcbiAgICB2YXIgazsgICAgICAgICAgICAgICAgICAgICAgIC8vIG51bWJlciBvZiBiaXRzIGluIGN1cnJlbnQgY29kZVxuICAgIHZhciBsOyAgICAgICAgICAgICAgICAgICAgICAgLy8gYml0cyBwZXIgdGFibGUgKHJldHVybmVkIGluIG0pXG4gICAgdmFyIG1hc2s7ICAgICAgICAgICAgICAgICAgICAvLyAoMSA8PCB3KSAtIDEsIHRvIGF2b2lkIGNjIC1PIGJ1ZyBvbiBIUFxuICAgIHZhciBwOyAgICAgICAgICAgICAgICAgICAgICAgLy8gcG9pbnRlciBpbnRvIGNbXSwgYltdLCBvciB2W11cbiAgICB2YXIgcTsgICAgICAgICAgICAgICAgICAgICAgIC8vIHBvaW50cyB0byBjdXJyZW50IHRhYmxlXG4gICAgdmFyIHc7ICAgICAgICAgICAgICAgICAgICAgICAvLyBiaXRzIGJlZm9yZSB0aGlzIHRhYmxlID09IChsICogaClcbiAgICB2YXIgeHA7ICAgICAgICAgICAgICAgICAgICAgIC8vIHBvaW50ZXIgaW50byB4XG4gICAgdmFyIHk7ICAgICAgICAgICAgICAgICAgICAgICAvLyBudW1iZXIgb2YgZHVtbXkgY29kZXMgYWRkZWRcbiAgICB2YXIgejsgICAgICAgICAgICAgICAgICAgICAgIC8vIG51bWJlciBvZiBlbnRyaWVzIGluIGN1cnJlbnQgdGFibGVcblxuICAgIC8vIEdlbmVyYXRlIGNvdW50cyBmb3IgZWFjaCBiaXQgbGVuZ3RoXG5cbiAgICBwID0gMDsgaSA9IG47XG4gICAgZG8ge1xuICAgICAgdGhpcy5jW2JbYmluZGV4K3BdXSsrOyBwKys7IGktLTsgICAvLyBhc3N1bWUgYWxsIGVudHJpZXMgPD0gQk1BWFxuICAgIH13aGlsZShpIT0wKTtcblxuICAgIGlmKHRoaXMuY1swXSA9PSBuKXsgICAgICAgICAgICAgICAgLy8gbnVsbCBpbnB1dC0tYWxsIHplcm8gbGVuZ3RoIGNvZGVzXG4gICAgICB0WzBdID0gLTE7XG4gICAgICBtWzBdID0gMDtcbiAgICAgIHJldHVybiBaX09LO1xuICAgIH1cblxuICAgIC8vIEZpbmQgbWluaW11bSBhbmQgbWF4aW11bSBsZW5ndGgsIGJvdW5kICptIGJ5IHRob3NlXG4gICAgbCA9IG1bMF07XG4gICAgZm9yIChqID0gMTsgaiA8PSBCTUFYOyBqKyspXG4gICAgICBpZih0aGlzLmNbal0hPTApIGJyZWFrO1xuICAgIGsgPSBqOyAgICAgICAgICAgICAgICAgICAgICAgIC8vIG1pbmltdW0gY29kZSBsZW5ndGhcbiAgICBpZihsIDwgail7XG4gICAgICBsID0gajtcbiAgICB9XG4gICAgZm9yIChpID0gQk1BWDsgaSE9MDsgaS0tKXtcbiAgICAgIGlmKHRoaXMuY1tpXSE9MCkgYnJlYWs7XG4gICAgfVxuICAgIGcgPSBpOyAgICAgICAgICAgICAgICAgICAgICAgIC8vIG1heGltdW0gY29kZSBsZW5ndGhcbiAgICBpZihsID4gaSl7XG4gICAgICBsID0gaTtcbiAgICB9XG4gICAgbVswXSA9IGw7XG5cbiAgICAvLyBBZGp1c3QgbGFzdCBsZW5ndGggY291bnQgdG8gZmlsbCBvdXQgY29kZXMsIGlmIG5lZWRlZFxuICAgIGZvciAoeSA9IDEgPDwgajsgaiA8IGk7IGorKywgeSA8PD0gMSl7XG4gICAgICBpZiAoKHkgLT0gdGhpcy5jW2pdKSA8IDApe1xuICAgICAgICByZXR1cm4gWl9EQVRBX0VSUk9SO1xuICAgICAgfVxuICAgIH1cbiAgICBpZiAoKHkgLT0gdGhpcy5jW2ldKSA8IDApe1xuICAgICAgcmV0dXJuIFpfREFUQV9FUlJPUjtcbiAgICB9XG4gICAgdGhpcy5jW2ldICs9IHk7XG5cbiAgICAvLyBHZW5lcmF0ZSBzdGFydGluZyBvZmZzZXRzIGludG8gdGhlIHZhbHVlIHRhYmxlIGZvciBlYWNoIGxlbmd0aFxuICAgIHRoaXMueFsxXSA9IGogPSAwO1xuICAgIHAgPSAxOyAgeHAgPSAyO1xuICAgIHdoaWxlICgtLWkhPTApIHsgICAgICAgICAgICAgICAgIC8vIG5vdGUgdGhhdCBpID09IGcgZnJvbSBhYm92ZVxuICAgICAgdGhpcy54W3hwXSA9IChqICs9IHRoaXMuY1twXSk7XG4gICAgICB4cCsrO1xuICAgICAgcCsrO1xuICAgIH1cblxuICAgIC8vIE1ha2UgYSB0YWJsZSBvZiB2YWx1ZXMgaW4gb3JkZXIgb2YgYml0IGxlbmd0aHNcbiAgICBpID0gMDsgcCA9IDA7XG4gICAgZG8ge1xuICAgICAgaWYgKChqID0gYltiaW5kZXgrcF0pICE9IDApe1xuICAgICAgICB0aGlzLnZbdGhpcy54W2pdKytdID0gaTtcbiAgICAgIH1cbiAgICAgIHArKztcbiAgICB9XG4gICAgd2hpbGUgKCsraSA8IG4pO1xuICAgIG4gPSB0aGlzLnhbZ107ICAgICAgICAgICAgICAgICAgICAgLy8gc2V0IG4gdG8gbGVuZ3RoIG9mIHZcblxuICAgIC8vIEdlbmVyYXRlIHRoZSBIdWZmbWFuIGNvZGVzIGFuZCBmb3IgZWFjaCwgbWFrZSB0aGUgdGFibGUgZW50cmllc1xuICAgIHRoaXMueFswXSA9IGkgPSAwOyAgICAgICAgICAgICAgICAgLy8gZmlyc3QgSHVmZm1hbiBjb2RlIGlzIHplcm9cbiAgICBwID0gMDsgICAgICAgICAgICAgICAgICAgICAgICAvLyBncmFiIHZhbHVlcyBpbiBiaXQgb3JkZXJcbiAgICBoID0gLTE7ICAgICAgICAgICAgICAgICAgICAgICAvLyBubyB0YWJsZXMgeWV0LS1sZXZlbCAtMVxuICAgIHcgPSAtbDsgICAgICAgICAgICAgICAgICAgICAgIC8vIGJpdHMgZGVjb2RlZCA9PSAobCAqIGgpXG4gICAgdGhpcy51WzBdID0gMDsgICAgICAgICAgICAgICAgICAgICAvLyBqdXN0IHRvIGtlZXAgY29tcGlsZXJzIGhhcHB5XG4gICAgcSA9IDA7ICAgICAgICAgICAgICAgICAgICAgICAgLy8gZGl0dG9cbiAgICB6ID0gMDsgICAgICAgICAgICAgICAgICAgICAgICAvLyBkaXR0b1xuXG4gICAgLy8gZ28gdGhyb3VnaCB0aGUgYml0IGxlbmd0aHMgKGsgYWxyZWFkeSBpcyBiaXRzIGluIHNob3J0ZXN0IGNvZGUpXG4gICAgZm9yICg7IGsgPD0gZzsgaysrKXtcbiAgICAgIGEgPSB0aGlzLmNba107XG4gICAgICB3aGlsZSAoYS0tIT0wKXtcblx0Ly8gaGVyZSBpIGlzIHRoZSBIdWZmbWFuIGNvZGUgb2YgbGVuZ3RoIGsgYml0cyBmb3IgdmFsdWUgKnBcblx0Ly8gbWFrZSB0YWJsZXMgdXAgdG8gcmVxdWlyZWQgbGV2ZWxcbiAgICAgICAgd2hpbGUgKGsgPiB3ICsgbCl7XG4gICAgICAgICAgaCsrO1xuICAgICAgICAgIHcgKz0gbDsgICAgICAgICAgICAgICAgIC8vIHByZXZpb3VzIHRhYmxlIGFsd2F5cyBsIGJpdHNcblx0ICAvLyBjb21wdXRlIG1pbmltdW0gc2l6ZSB0YWJsZSBsZXNzIHRoYW4gb3IgZXF1YWwgdG8gbCBiaXRzXG4gICAgICAgICAgeiA9IGcgLSB3O1xuICAgICAgICAgIHogPSAoeiA+IGwpID8gbCA6IHo7ICAgICAgICAvLyB0YWJsZSBzaXplIHVwcGVyIGxpbWl0XG4gICAgICAgICAgaWYoKGY9MTw8KGo9ay13KSk+YSsxKXsgICAgIC8vIHRyeSBhIGstdyBiaXQgdGFibGVcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLy8gdG9vIGZldyBjb2RlcyBmb3Igay13IGJpdCB0YWJsZVxuICAgICAgICAgICAgZiAtPSBhICsgMTsgICAgICAgICAgICAgICAvLyBkZWR1Y3QgY29kZXMgZnJvbSBwYXR0ZXJucyBsZWZ0XG4gICAgICAgICAgICB4cCA9IGs7XG4gICAgICAgICAgICBpZihqIDwgeil7XG4gICAgICAgICAgICAgIHdoaWxlICgrK2ogPCB6KXsgICAgICAgIC8vIHRyeSBzbWFsbGVyIHRhYmxlcyB1cCB0byB6IGJpdHNcbiAgICAgICAgICAgICAgICBpZigoZiA8PD0gMSkgPD0gdGhpcy5jWysreHBdKVxuICAgICAgICAgICAgICAgICAgYnJlYWs7ICAgICAgICAgICAgICAvLyBlbm91Z2ggY29kZXMgdG8gdXNlIHVwIGogYml0c1xuICAgICAgICAgICAgICAgIGYgLT0gdGhpcy5jW3hwXTsgICAgICAgICAgIC8vIGVsc2UgZGVkdWN0IGNvZGVzIGZyb20gcGF0dGVybnNcbiAgICAgICAgICAgICAgfVxuXHQgICAgfVxuICAgICAgICAgIH1cbiAgICAgICAgICB6ID0gMSA8PCBqOyAgICAgICAgICAgICAgICAgLy8gdGFibGUgZW50cmllcyBmb3Igai1iaXQgdGFibGVcblxuXHQgIC8vIGFsbG9jYXRlIG5ldyB0YWJsZVxuICAgICAgICAgIGlmICh0aGlzLmhuWzBdICsgeiA+IE1BTlkpeyAgICAgICAvLyAobm90ZTogZG9lc24ndCBtYXR0ZXIgZm9yIGZpeGVkKVxuICAgICAgICAgICAgcmV0dXJuIFpfREFUQV9FUlJPUjsgICAgICAgLy8gb3ZlcmZsb3cgb2YgTUFOWVxuICAgICAgICAgIH1cbiAgICAgICAgICB0aGlzLnVbaF0gPSBxID0gLypocCsqLyB0aGlzLmhuWzBdOyAgIC8vIERFQlVHXG4gICAgICAgICAgdGhpcy5oblswXSArPSB6O1xuIFxuXHQgIC8vIGNvbm5lY3QgdG8gbGFzdCB0YWJsZSwgaWYgdGhlcmUgaXMgb25lXG5cdCAgaWYoaCE9MCl7XG4gICAgICAgICAgICB0aGlzLnhbaF09aTsgICAgICAgICAgIC8vIHNhdmUgcGF0dGVybiBmb3IgYmFja2luZyB1cFxuICAgICAgICAgICAgdGhpcy5yWzBdPWo7ICAgICAvLyBiaXRzIGluIHRoaXMgdGFibGVcbiAgICAgICAgICAgIHRoaXMuclsxXT1sOyAgICAgLy8gYml0cyB0byBkdW1wIGJlZm9yZSB0aGlzIHRhYmxlXG4gICAgICAgICAgICBqPWk+Pj4odyAtIGwpO1xuICAgICAgICAgICAgdGhpcy5yWzJdID0gKHEgLSB0aGlzLnVbaC0xXSAtIGopOyAgICAgICAgICAgICAgIC8vIG9mZnNldCB0byB0aGlzIHRhYmxlXG4gICAgICAgICAgICBhcnJheUNvcHkodGhpcy5yLCAwLCBocCwgKHRoaXMudVtoLTFdK2opKjMsIDMpOyAvLyBjb25uZWN0IHRvIGxhc3QgdGFibGVcbiAgICAgICAgICB9XG4gICAgICAgICAgZWxzZXtcbiAgICAgICAgICAgIHRbMF0gPSBxOyAgICAgICAgICAgICAgIC8vIGZpcnN0IHRhYmxlIGlzIHJldHVybmVkIHJlc3VsdFxuXHQgIH1cbiAgICAgICAgfVxuXG5cdC8vIHNldCB1cCB0YWJsZSBlbnRyeSBpbiByXG4gICAgICAgIHRoaXMuclsxXSA9IChrIC0gdyk7XG4gICAgICAgIGlmIChwID49IG4pe1xuICAgICAgICAgIHRoaXMuclswXSA9IDEyOCArIDY0OyAgICAgIC8vIG91dCBvZiB2YWx1ZXMtLWludmFsaWQgY29kZVxuXHR9XG4gICAgICAgIGVsc2UgaWYgKHZbcF0gPCBzKXtcbiAgICAgICAgICB0aGlzLnJbMF0gPSAodGhpcy52W3BdIDwgMjU2ID8gMCA6IDMyICsgNjQpOyAgLy8gMjU2IGlzIGVuZC1vZi1ibG9ja1xuICAgICAgICAgIHRoaXMuclsyXSA9IHRoaXMudltwKytdOyAgICAgICAgICAvLyBzaW1wbGUgY29kZSBpcyBqdXN0IHRoZSB2YWx1ZVxuICAgICAgICB9XG4gICAgICAgIGVsc2V7XG4gICAgICAgICAgdGhpcy5yWzBdPShlW3RoaXMudltwXS1zXSsxNis2NCk7IC8vIG5vbi1zaW1wbGUtLWxvb2sgdXAgaW4gbGlzdHNcbiAgICAgICAgICB0aGlzLnJbMl09ZFt0aGlzLnZbcCsrXSAtIHNdO1xuICAgICAgICB9XG5cbiAgICAgICAgLy8gZmlsbCBjb2RlLWxpa2UgZW50cmllcyB3aXRoIHJcbiAgICAgICAgZj0xPDwoay13KTtcbiAgICAgICAgZm9yIChqPWk+Pj53O2o8ejtqKz1mKXtcbiAgICAgICAgICBhcnJheUNvcHkodGhpcy5yLCAwLCBocCwgKHEraikqMywgMyk7XG5cdH1cblxuXHQvLyBiYWNrd2FyZHMgaW5jcmVtZW50IHRoZSBrLWJpdCBjb2RlIGlcbiAgICAgICAgZm9yIChqID0gMSA8PCAoayAtIDEpOyAoaSAmIGopIT0wOyBqID4+Pj0gMSl7XG4gICAgICAgICAgaSBePSBqO1xuXHR9XG4gICAgICAgIGkgXj0gajtcblxuXHQvLyBiYWNrdXAgb3ZlciBmaW5pc2hlZCB0YWJsZXNcbiAgICAgICAgbWFzayA9ICgxIDw8IHcpIC0gMTsgICAgICAvLyBuZWVkZWQgb24gSFAsIGNjIC1PIGJ1Z1xuICAgICAgICB3aGlsZSAoKGkgJiBtYXNrKSAhPSB0aGlzLnhbaF0pe1xuICAgICAgICAgIGgtLTsgICAgICAgICAgICAgICAgICAgIC8vIGRvbid0IG5lZWQgdG8gdXBkYXRlIHFcbiAgICAgICAgICB3IC09IGw7XG4gICAgICAgICAgbWFzayA9ICgxIDw8IHcpIC0gMTtcbiAgICAgICAgfVxuICAgICAgfVxuICAgIH1cbiAgICAvLyBSZXR1cm4gWl9CVUZfRVJST1IgaWYgd2Ugd2VyZSBnaXZlbiBhbiBpbmNvbXBsZXRlIHRhYmxlXG4gICAgcmV0dXJuIHkgIT0gMCAmJiBnICE9IDEgPyBaX0JVRl9FUlJPUiA6IFpfT0s7XG59XG5cbkluZlRyZWUucHJvdG90eXBlLmluZmxhdGVfdHJlZXNfYml0cyA9IGZ1bmN0aW9uKGMsIGJiLCB0YiwgaHAsIHopIHtcbiAgICB2YXIgcmVzdWx0O1xuICAgIHRoaXMuaW5pdFdvcmtBcmVhKDE5KTtcbiAgICB0aGlzLmhuWzBdPTA7XG4gICAgcmVzdWx0ID0gdGhpcy5odWZ0X2J1aWxkKGMsIDAsIDE5LCAxOSwgbnVsbCwgbnVsbCwgdGIsIGJiLCBocCwgdGhpcy5obiwgdGhpcy52KTtcblxuICAgIGlmKHJlc3VsdCA9PSBaX0RBVEFfRVJST1Ipe1xuICAgICAgei5tc2cgPSBcIm92ZXJzdWJzY3JpYmVkIGR5bmFtaWMgYml0IGxlbmd0aHMgdHJlZVwiO1xuICAgIH1cbiAgICBlbHNlIGlmKHJlc3VsdCA9PSBaX0JVRl9FUlJPUiB8fCBiYlswXSA9PSAwKXtcbiAgICAgIHoubXNnID0gXCJpbmNvbXBsZXRlIGR5bmFtaWMgYml0IGxlbmd0aHMgdHJlZVwiO1xuICAgICAgcmVzdWx0ID0gWl9EQVRBX0VSUk9SO1xuICAgIH1cbiAgICByZXR1cm4gcmVzdWx0O1xufVxuXG5JbmZUcmVlLnByb3RvdHlwZS5pbmZsYXRlX3RyZWVzX2R5bmFtaWMgPSBmdW5jdGlvbihubCwgbmQsIGMsIGJsLCBiZCwgdGwsIHRkLCBocCwgeikge1xuICAgIHZhciByZXN1bHQ7XG5cbiAgICAvLyBidWlsZCBsaXRlcmFsL2xlbmd0aCB0cmVlXG4gICAgdGhpcy5pbml0V29ya0FyZWEoMjg4KTtcbiAgICB0aGlzLmhuWzBdPTA7XG4gICAgcmVzdWx0ID0gdGhpcy5odWZ0X2J1aWxkKGMsIDAsIG5sLCAyNTcsIGNwbGVucywgY3BsZXh0LCB0bCwgYmwsIGhwLCB0aGlzLmhuLCB0aGlzLnYpO1xuICAgIGlmIChyZXN1bHQgIT0gWl9PSyB8fCBibFswXSA9PSAwKXtcbiAgICAgIGlmKHJlc3VsdCA9PSBaX0RBVEFfRVJST1Ipe1xuICAgICAgICB6Lm1zZyA9IFwib3ZlcnN1YnNjcmliZWQgbGl0ZXJhbC9sZW5ndGggdHJlZVwiO1xuICAgICAgfVxuICAgICAgZWxzZSBpZiAocmVzdWx0ICE9IFpfTUVNX0VSUk9SKXtcbiAgICAgICAgei5tc2cgPSBcImluY29tcGxldGUgbGl0ZXJhbC9sZW5ndGggdHJlZVwiO1xuICAgICAgICByZXN1bHQgPSBaX0RBVEFfRVJST1I7XG4gICAgICB9XG4gICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cblxuICAgIC8vIGJ1aWxkIGRpc3RhbmNlIHRyZWVcbiAgICB0aGlzLmluaXRXb3JrQXJlYSgyODgpO1xuICAgIHJlc3VsdCA9IHRoaXMuaHVmdF9idWlsZChjLCBubCwgbmQsIDAsIGNwZGlzdCwgY3BkZXh0LCB0ZCwgYmQsIGhwLCB0aGlzLmhuLCB0aGlzLnYpO1xuXG4gICAgaWYgKHJlc3VsdCAhPSBaX09LIHx8IChiZFswXSA9PSAwICYmIG5sID4gMjU3KSl7XG4gICAgICBpZiAocmVzdWx0ID09IFpfREFUQV9FUlJPUil7XG4gICAgICAgIHoubXNnID0gXCJvdmVyc3Vic2NyaWJlZCBkaXN0YW5jZSB0cmVlXCI7XG4gICAgICB9XG4gICAgICBlbHNlIGlmIChyZXN1bHQgPT0gWl9CVUZfRVJST1IpIHtcbiAgICAgICAgei5tc2cgPSBcImluY29tcGxldGUgZGlzdGFuY2UgdHJlZVwiO1xuICAgICAgICByZXN1bHQgPSBaX0RBVEFfRVJST1I7XG4gICAgICB9XG4gICAgICBlbHNlIGlmIChyZXN1bHQgIT0gWl9NRU1fRVJST1Ipe1xuICAgICAgICB6Lm1zZyA9IFwiZW1wdHkgZGlzdGFuY2UgdHJlZSB3aXRoIGxlbmd0aHNcIjtcbiAgICAgICAgcmVzdWx0ID0gWl9EQVRBX0VSUk9SO1xuICAgICAgfVxuICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICB9XG5cbiAgICByZXR1cm4gWl9PSztcbn1cbi8qXG4gIHN0YXRpYyBpbnQgaW5mbGF0ZV90cmVlc19maXhlZChpbnRbXSBibCwgIC8vbGl0ZXJhbCBkZXNpcmVkL2FjdHVhbCBiaXQgZGVwdGhcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGludFtdIGJkLCAgLy9kaXN0YW5jZSBkZXNpcmVkL2FjdHVhbCBiaXQgZGVwdGhcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGludFtdW10gdGwsLy9saXRlcmFsL2xlbmd0aCB0cmVlIHJlc3VsdFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaW50W11bXSB0ZCwvL2Rpc3RhbmNlIHRyZWUgcmVzdWx0IFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgWlN0cmVhbSB6ICAvL2ZvciBtZW1vcnkgYWxsb2NhdGlvblxuXHRcdFx0XHQgKXtcblxuKi9cblxuZnVuY3Rpb24gaW5mbGF0ZV90cmVlc19maXhlZChibCwgYmQsIHRsLCB0ZCwgeikge1xuICAgIGJsWzBdPWZpeGVkX2JsO1xuICAgIGJkWzBdPWZpeGVkX2JkO1xuICAgIHRsWzBdPWZpeGVkX3RsO1xuICAgIHRkWzBdPWZpeGVkX3RkO1xuICAgIHJldHVybiBaX09LO1xufVxuXG5JbmZUcmVlLnByb3RvdHlwZS5pbml0V29ya0FyZWEgPSBmdW5jdGlvbih2c2l6ZSl7XG4gICAgaWYodGhpcy5obj09bnVsbCl7XG4gICAgICAgIHRoaXMuaG49bmV3IEludDMyQXJyYXkoMSk7XG4gICAgICAgIHRoaXMudj1uZXcgSW50MzJBcnJheSh2c2l6ZSk7XG4gICAgICAgIHRoaXMuYz1uZXcgSW50MzJBcnJheShCTUFYKzEpO1xuICAgICAgICB0aGlzLnI9bmV3IEludDMyQXJyYXkoMyk7XG4gICAgICAgIHRoaXMudT1uZXcgSW50MzJBcnJheShCTUFYKTtcbiAgICAgICAgdGhpcy54PW5ldyBJbnQzMkFycmF5KEJNQVgrMSk7XG4gICAgfVxuICAgIGlmKHRoaXMudi5sZW5ndGg8dnNpemUpeyBcbiAgICAgICAgdGhpcy52PW5ldyBJbnQzMkFycmF5KHZzaXplKTsgXG4gICAgfVxuICAgIGZvcih2YXIgaT0wOyBpPHZzaXplOyBpKyspe3RoaXMudltpXT0wO31cbiAgICBmb3IodmFyIGk9MDsgaTxCTUFYKzE7IGkrKyl7dGhpcy5jW2ldPTA7fVxuICAgIGZvcih2YXIgaT0wOyBpPDM7IGkrKyl7dGhpcy5yW2ldPTA7fVxuLy8gIGZvcihpbnQgaT0wOyBpPEJNQVg7IGkrKyl7dVtpXT0wO31cbiAgICBhcnJheUNvcHkodGhpcy5jLCAwLCB0aGlzLnUsIDAsIEJNQVgpO1xuLy8gIGZvcihpbnQgaT0wOyBpPEJNQVgrMTsgaSsrKXt4W2ldPTA7fVxuICAgIGFycmF5Q29weSh0aGlzLmMsIDAsIHRoaXMueCwgMCwgQk1BWCsxKTtcbn1cblxudmFyIHRlc3RBcnJheSA9IG5ldyBVaW50OEFycmF5KDEpO1xudmFyIGhhc1N1YmFycmF5ID0gKHR5cGVvZiB0ZXN0QXJyYXkuc3ViYXJyYXkgPT09ICdmdW5jdGlvbicpO1xudmFyIGhhc1NsaWNlID0gZmFsc2U7IC8qICh0eXBlb2YgdGVzdEFycmF5LnNsaWNlID09PSAnZnVuY3Rpb24nKTsgKi8gLy8gQ2hyb21lIHNsaWNlIHBlcmZvcm1hbmNlIGlzIHNvIGRpcmUgdGhhdCB3ZSdyZSBjdXJyZW50bHkgbm90IHVzaW5nIGl0Li4uXG5cbmZ1bmN0aW9uIGFycmF5Q29weShzcmMsIHNyY09mZnNldCwgZGVzdCwgZGVzdE9mZnNldCwgY291bnQpIHtcbiAgICBpZiAoY291bnQgPT0gMCkge1xuICAgICAgICByZXR1cm47XG4gICAgfSBcbiAgICBpZiAoIXNyYykge1xuICAgICAgICB0aHJvdyBcIlVuZGVmIHNyY1wiO1xuICAgIH0gZWxzZSBpZiAoIWRlc3QpIHtcbiAgICAgICAgdGhyb3cgXCJVbmRlZiBkZXN0XCI7XG4gICAgfVxuXG4gICAgaWYgKHNyY09mZnNldCA9PSAwICYmIGNvdW50ID09IHNyYy5sZW5ndGgpIHtcbiAgICAgICAgYXJyYXlDb3B5X2Zhc3Qoc3JjLCBkZXN0LCBkZXN0T2Zmc2V0KTtcbiAgICB9IGVsc2UgaWYgKGhhc1N1YmFycmF5KSB7XG4gICAgICAgIGFycmF5Q29weV9mYXN0KHNyYy5zdWJhcnJheShzcmNPZmZzZXQsIHNyY09mZnNldCArIGNvdW50KSwgZGVzdCwgZGVzdE9mZnNldCk7IFxuICAgIH0gZWxzZSBpZiAoc3JjLkJZVEVTX1BFUl9FTEVNRU5UID09IDEgJiYgY291bnQgPiAxMDApIHtcbiAgICAgICAgYXJyYXlDb3B5X2Zhc3QobmV3IFVpbnQ4QXJyYXkoc3JjLmJ1ZmZlciwgc3JjLmJ5dGVPZmZzZXQgKyBzcmNPZmZzZXQsIGNvdW50KSwgZGVzdCwgZGVzdE9mZnNldCk7XG4gICAgfSBlbHNlIHsgXG4gICAgICAgIGFycmF5Q29weV9zbG93KHNyYywgc3JjT2Zmc2V0LCBkZXN0LCBkZXN0T2Zmc2V0LCBjb3VudCk7XG4gICAgfVxuXG59XG5cbmZ1bmN0aW9uIGFycmF5Q29weV9zbG93KHNyYywgc3JjT2Zmc2V0LCBkZXN0LCBkZXN0T2Zmc2V0LCBjb3VudCkge1xuXG4gICAgLy8gZGxvZygnX3Nsb3cgY2FsbDogc3JjT2Zmc2V0PScgKyBzcmNPZmZzZXQgKyAnOyBkZXN0T2Zmc2V0PScgKyBkZXN0T2Zmc2V0ICsgJzsgY291bnQ9JyArIGNvdW50KTtcblxuICAgICBmb3IgKHZhciBpID0gMDsgaSA8IGNvdW50OyArK2kpIHtcbiAgICAgICAgZGVzdFtkZXN0T2Zmc2V0ICsgaV0gPSBzcmNbc3JjT2Zmc2V0ICsgaV07XG4gICAgfVxufVxuXG5mdW5jdGlvbiBhcnJheUNvcHlfZmFzdChzcmMsIGRlc3QsIGRlc3RPZmZzZXQpIHtcbiAgICBkZXN0LnNldChzcmMsIGRlc3RPZmZzZXQpO1xufVxuXG5cbiAgLy8gbGFyZ2VzdCBwcmltZSBzbWFsbGVyIHRoYW4gNjU1MzZcbnZhciBBRExFUl9CQVNFPTY1NTIxOyBcbiAgLy8gTk1BWCBpcyB0aGUgbGFyZ2VzdCBuIHN1Y2ggdGhhdCAyNTVuKG4rMSkvMiArIChuKzEpKEJBU0UtMSkgPD0gMl4zMi0xXG52YXIgQURMRVJfTk1BWD01NTUyO1xuXG5mdW5jdGlvbiBhZGxlcjMyKGFkbGVyLCAvKiBieXRlW10gKi8gYnVmLCAgaW5kZXgsIGxlbil7XG4gICAgaWYoYnVmID09IG51bGwpeyByZXR1cm4gMTsgfVxuXG4gICAgdmFyIHMxPWFkbGVyJjB4ZmZmZjtcbiAgICB2YXIgczI9KGFkbGVyPj4xNikmMHhmZmZmO1xuICAgIHZhciBrO1xuXG4gICAgd2hpbGUobGVuID4gMCkge1xuICAgICAgaz1sZW48QURMRVJfTk1BWD9sZW46QURMRVJfTk1BWDtcbiAgICAgIGxlbi09aztcbiAgICAgIHdoaWxlKGs+PTE2KXtcbiAgICAgICAgczErPWJ1ZltpbmRleCsrXSYweGZmOyBzMis9czE7XG4gICAgICAgIHMxKz1idWZbaW5kZXgrK10mMHhmZjsgczIrPXMxO1xuICAgICAgICBzMSs9YnVmW2luZGV4KytdJjB4ZmY7IHMyKz1zMTtcbiAgICAgICAgczErPWJ1ZltpbmRleCsrXSYweGZmOyBzMis9czE7XG4gICAgICAgIHMxKz1idWZbaW5kZXgrK10mMHhmZjsgczIrPXMxO1xuICAgICAgICBzMSs9YnVmW2luZGV4KytdJjB4ZmY7IHMyKz1zMTtcbiAgICAgICAgczErPWJ1ZltpbmRleCsrXSYweGZmOyBzMis9czE7XG4gICAgICAgIHMxKz1idWZbaW5kZXgrK10mMHhmZjsgczIrPXMxO1xuICAgICAgICBzMSs9YnVmW2luZGV4KytdJjB4ZmY7IHMyKz1zMTtcbiAgICAgICAgczErPWJ1ZltpbmRleCsrXSYweGZmOyBzMis9czE7XG4gICAgICAgIHMxKz1idWZbaW5kZXgrK10mMHhmZjsgczIrPXMxO1xuICAgICAgICBzMSs9YnVmW2luZGV4KytdJjB4ZmY7IHMyKz1zMTtcbiAgICAgICAgczErPWJ1ZltpbmRleCsrXSYweGZmOyBzMis9czE7XG4gICAgICAgIHMxKz1idWZbaW5kZXgrK10mMHhmZjsgczIrPXMxO1xuICAgICAgICBzMSs9YnVmW2luZGV4KytdJjB4ZmY7IHMyKz1zMTtcbiAgICAgICAgczErPWJ1ZltpbmRleCsrXSYweGZmOyBzMis9czE7XG4gICAgICAgIGstPTE2O1xuICAgICAgfVxuICAgICAgaWYoayE9MCl7XG4gICAgICAgIGRve1xuICAgICAgICAgIHMxKz1idWZbaW5kZXgrK10mMHhmZjsgczIrPXMxO1xuICAgICAgICB9XG4gICAgICAgIHdoaWxlKC0tayE9MCk7XG4gICAgICB9XG4gICAgICBzMSU9QURMRVJfQkFTRTtcbiAgICAgIHMyJT1BRExFUl9CQVNFO1xuICAgIH1cbiAgICByZXR1cm4gKHMyPDwxNil8czE7XG59XG5cblxuXG5mdW5jdGlvbiBqc3psaWJfaW5mbGF0ZV9idWZmZXIoYnVmZmVyLCBzdGFydCwgbGVuZ3RoLCBhZnRlclVuY09mZnNldCkge1xuICAgIGlmICghc3RhcnQpIHtcbiAgICAgICAgYnVmZmVyID0gbmV3IFVpbnQ4QXJyYXkoYnVmZmVyKTtcbiAgICB9IGVsc2Uge1xuICAgICAgICBidWZmZXIgPSBuZXcgVWludDhBcnJheShidWZmZXIsIHN0YXJ0LCBsZW5ndGgpO1xuICAgIH1cblxuICAgIHZhciB6ID0gbmV3IFpTdHJlYW0oKTtcbiAgICB6LmluZmxhdGVJbml0KERFRl9XQklUUywgdHJ1ZSk7XG4gICAgei5uZXh0X2luID0gYnVmZmVyO1xuICAgIHoubmV4dF9pbl9pbmRleCA9IDA7XG4gICAgei5hdmFpbF9pbiA9IGJ1ZmZlci5sZW5ndGg7XG5cbiAgICB2YXIgb0Jsb2NrTGlzdCA9IFtdO1xuICAgIHZhciB0b3RhbFNpemUgPSAwO1xuICAgIHdoaWxlICh0cnVlKSB7XG4gICAgICAgIHZhciBvYnVmID0gbmV3IFVpbnQ4QXJyYXkoMzIwMDApO1xuICAgICAgICB6Lm5leHRfb3V0ID0gb2J1ZjtcbiAgICAgICAgei5uZXh0X291dF9pbmRleCA9IDA7XG4gICAgICAgIHouYXZhaWxfb3V0ID0gb2J1Zi5sZW5ndGg7XG4gICAgICAgIHZhciBzdGF0dXMgPSB6LmluZmxhdGUoWl9OT19GTFVTSCk7XG4gICAgICAgIGlmIChzdGF0dXMgIT0gWl9PSyAmJiBzdGF0dXMgIT0gWl9TVFJFQU1fRU5EICYmIHN0YXR1cyAhPSBaX0JVRl9FUlJPUikge1xuICAgICAgICAgICAgdGhyb3cgei5tc2c7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHouYXZhaWxfb3V0ICE9IDApIHtcbiAgICAgICAgICAgIHZhciBuZXdvYiA9IG5ldyBVaW50OEFycmF5KG9idWYubGVuZ3RoIC0gei5hdmFpbF9vdXQpO1xuICAgICAgICAgICAgYXJyYXlDb3B5KG9idWYsIDAsIG5ld29iLCAwLCAob2J1Zi5sZW5ndGggLSB6LmF2YWlsX291dCkpO1xuICAgICAgICAgICAgb2J1ZiA9IG5ld29iO1xuICAgICAgICB9XG4gICAgICAgIG9CbG9ja0xpc3QucHVzaChvYnVmKTtcbiAgICAgICAgdG90YWxTaXplICs9IG9idWYubGVuZ3RoO1xuICAgICAgICBpZiAoc3RhdHVzID09IFpfU1RSRUFNX0VORCB8fCBzdGF0dXMgPT0gWl9CVUZfRVJST1IpIHtcbiAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICB9XG4gICAgfVxuXG4gICAgaWYgKGFmdGVyVW5jT2Zmc2V0KSB7XG4gICAgICAgIGFmdGVyVW5jT2Zmc2V0WzBdID0gKHN0YXJ0IHx8IDApICsgei5uZXh0X2luX2luZGV4O1xuICAgIH1cblxuICAgIGlmIChvQmxvY2tMaXN0Lmxlbmd0aCA9PSAxKSB7XG4gICAgICAgIHJldHVybiBvQmxvY2tMaXN0WzBdLmJ1ZmZlcjtcbiAgICB9IGVsc2Uge1xuICAgICAgICB2YXIgb3V0ID0gbmV3IFVpbnQ4QXJyYXkodG90YWxTaXplKTtcbiAgICAgICAgdmFyIGN1cnNvciA9IDA7XG4gICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgb0Jsb2NrTGlzdC5sZW5ndGg7ICsraSkge1xuICAgICAgICAgICAgdmFyIGIgPSBvQmxvY2tMaXN0W2ldO1xuICAgICAgICAgICAgYXJyYXlDb3B5KGIsIDAsIG91dCwgY3Vyc29yLCBiLmxlbmd0aCk7XG4gICAgICAgICAgICBjdXJzb3IgKz0gYi5sZW5ndGg7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIG91dC5idWZmZXI7XG4gICAgfVxufVxuXG5pZiAodHlwZW9mKG1vZHVsZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gIG1vZHVsZS5leHBvcnRzID0ge1xuICAgIGluZmxhdGVCdWZmZXI6IGpzemxpYl9pbmZsYXRlX2J1ZmZlcixcbiAgICBhcnJheUNvcHk6IGFycmF5Q29weVxuICB9O1xufSJdfQ==
