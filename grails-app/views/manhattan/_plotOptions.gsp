<!--  Another DIV for the manhattan plot options. -->
<div id="divPlotOptions" style="width:300px; display: none;">
    <table class="columndetail">
        <tr>
            <td class="columnname">SNP Annotation Source</td>
            <td>
                <select id="plotSnpSource" style="width: 220px">
                    <option value="19">Human Genome 19</option>
                    <option value="18">Human Genome 18</option>
                </select>
            </td>
        </tr>
        <%--<tr>
            <td class="columnname">Gene Annotation Source</td>
            <td>
                <select id="plotGeneSource" style="width: 220px">
                    <option id="GRCh37">Human Gene data from NCBI</option>
                </select>
            </td>
        </tr>--%>
        <tr>
            <td class="columnname">P-value cutoff</td>
            <td>
                <input id="plotPvalueCutoff" style="width: 210px">
            </td>
        </tr>
    </table>
</div>