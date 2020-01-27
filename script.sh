listaOS="$(ls -d */)"

for os in $listaOS; do
	cd $os

	listaProgramas="$(ls -d */)"

	for prog in $listaProgramas; do
		cd $prog
		pwd

		job=$(find *-service/src/main/resources/br/* -name '*.xml')
		# echo $job

		query=$(find *-service/src/main/resources/* -name 'sql_queries.xml')
		# echo $query

		cd ../../

		java -jar spring_batch_smell_detector.jar -s $os$prog $os$prog$job $os$prog$query

		fileName="metrics_${os:0:-1}_${prog:0:-1}.csv"
		queryName="query_metrics_${os:0:-1}_${prog:0:-1}.csv"

		mv metrics.csv $fileName
		mv query_metrics.csv $queryName

		cd $os
	done

	cd /home/deyvisson/dev/git/spring_bactch_smell_detector/execution
done

cat metrics_*.csv > metrics_global.csv
cat query_metrics_*.csv > query_metrics_global.csv